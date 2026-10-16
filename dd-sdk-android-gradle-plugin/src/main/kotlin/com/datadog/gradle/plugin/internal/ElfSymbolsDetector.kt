/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal

import com.datadog.gradle.plugin.DdAndroidGradlePlugin.Companion.LOGGER
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Tells whether a native library still has the symbols needed for symbolication, by reading the
 * types and names of the sections listed in its ELF section header table.
 */
@Suppress("MagicNumber")
internal object ElfSymbolsDetector {

    enum class Result {
        /** The library has a symbol table or debug info. */
        HAS_SYMBOLS,

        /** The library is a valid ELF file without symbol table and debug info. */
        STRIPPED,

        /** The file is not an ELF file, or it can't be read. */
        UNKNOWN
    }

    fun detect(file: File): Result {
        return try {
            RandomAccessFile(file, "r").use { detect(it) }
        } catch (e: IOException) {
            onReadError(file, e)
        } catch (e: SecurityException) {
            onReadError(file, e)
        }
    }

    // region Private

    private fun onReadError(file: File, e: Exception): Result {
        LOGGER.info("Unable to read native library ${file.absolutePath} to check its symbols", e)
        return Result.UNKNOWN
    }

    // any inconsistency in the file structure ends the detection early
    @Suppress("ReturnCount")
    private fun detect(file: RandomAccessFile): Result {
        val header = file.read(0, ELF64_HEADER_SIZE) ?: return Result.UNKNOWN
        if (!ELF_MAGIC.indices.all { header.get(it) == ELF_MAGIC[it] }) return Result.UNKNOWN
        val is64Bit = when (header.get(EI_CLASS).toInt()) {
            ELFCLASS32 -> false
            ELFCLASS64 -> true
            else -> return Result.UNKNOWN
        }
        when (header.get(EI_DATA).toInt()) {
            ELFDATA2LSB -> header.order(ByteOrder.LITTLE_ENDIAN)
            ELFDATA2MSB -> header.order(ByteOrder.BIG_ENDIAN)
            else -> return Result.UNKNOWN
        }

        val layout = if (is64Bit) ELF64_LAYOUT else ELF32_LAYOUT
        val sectionHeadersOffset = header.readAddress(layout.shoffOffset, is64Bit)
        val sectionHeaderSize = header.getShort(layout.shentsizeOffset).toUnsignedInt()
        val sectionsCount = header.getShort(layout.shnumOffset).toUnsignedInt()
        val namesSectionIndex = header.getShort(layout.shstrndxOffset).toUnsignedInt()
        // `strip --strip-section-headers` drops the section header table, which implies stripping all the symbols:
        // only the dynamic symbols reachable through the program headers are left, as for any stripped library
        if (sectionHeadersOffset == 0L && sectionsCount == 0) return Result.STRIPPED
        // extended numbering (more than 0xff00 sections) is not supported, such libraries are not expected.
        // The section header size is fixed by the ELF class, checking it bounds the section header table size.
        if (sectionsCount == 0 ||
            sectionHeaderSize != layout.sectionHeaderSize ||
            namesSectionIndex >= sectionsCount
        ) {
            return Result.UNKNOWN
        }

        val sectionHeaders = file.read(sectionHeadersOffset, sectionsCount.toLong() * sectionHeaderSize)
            ?.order(header.order())
            ?: return Result.UNKNOWN
        return detect(file, sectionHeaders, sectionsCount, namesSectionIndex, layout, is64Bit)
    }

    @Suppress("ReturnCount", "LongParameterList")
    private fun detect(
        file: RandomAccessFile,
        sectionHeaders: ByteBuffer,
        sectionsCount: Int,
        namesSectionIndex: Int,
        layout: Layout,
        is64Bit: Boolean
    ): Result {
        // the symbol table is found by its type, as symbolic does, so it doesn't depend on the section names
        val hasSymbolTable = (0 until sectionsCount).any { index ->
            sectionHeaders.getInt(index * layout.sectionHeaderSize + SH_TYPE_OFFSET) == SHT_SYMTAB
        }
        if (hasSymbolTable) return Result.HAS_SYMBOLS
        // debug info sections are only found by their names, which can't be checked without the names table
        if (namesSectionIndex == SHN_UNDEF) return Result.UNKNOWN

        val namesHeaderOffset = namesSectionIndex * layout.sectionHeaderSize
        val namesSize = sectionHeaders.readAddress(namesHeaderOffset + layout.shSizeOffset, is64Bit)
        if (namesSize > MAX_NAMES_SECTION_SIZE) return Result.UNKNOWN
        val names = file.read(
            sectionHeaders.readAddress(namesHeaderOffset + layout.shOffsetOffset, is64Bit),
            namesSize
        ) ?: return Result.UNKNOWN

        val hasDebugInfo = (0 until sectionsCount).any { index ->
            val nameOffset = sectionHeaders.getInt(index * layout.sectionHeaderSize).toUnsignedLong()
            DEBUG_INFO_SECTION_NAMES.any { names.hasNameAt(nameOffset, it) }
        }
        return if (hasDebugInfo) Result.HAS_SYMBOLS else Result.STRIPPED
    }

    private fun RandomAccessFile.read(offset: Long, size: Long): ByteBuffer? {
        val isSizeValid = size in 0..Int.MAX_VALUE
        if (!isSizeValid || offset !in 0..length() - size) return null
        val bytes = ByteArray(size.toInt())
        seek(offset)
        readFully(bytes)
        return ByteBuffer.wrap(bytes)
    }

    private fun ByteBuffer.readAddress(offset: Int, is64Bit: Boolean): Long {
        // a negative value (> Long.MAX_VALUE as unsigned) is rejected later as out of the file bounds
        return if (is64Bit) getLong(offset) else getInt(offset).toUnsignedLong()
    }

    // compares at most name.size + 1 bytes, so that many sections pointing at a long unterminated
    // name don't make the check scan the whole table for each of them
    private fun ByteBuffer.hasNameAt(offset: Long, name: ByteArray): Boolean {
        if (offset < 0 || offset + name.size >= limit()) return false
        val start = offset.toInt()
        return name.indices.all { get(start + it) == name[it] } && get(start + name.size) == 0.toByte()
    }

    private fun Short.toUnsignedInt(): Int = toInt() and 0xFFFF

    private fun Int.toUnsignedLong(): Long = toLong() and 0xFFFFFFFFL

    private class Layout(
        val shoffOffset: Int,
        val shentsizeOffset: Int,
        val shnumOffset: Int,
        val shstrndxOffset: Int,
        val sectionHeaderSize: Int,
        val shOffsetOffset: Int,
        val shSizeOffset: Int
    )

    // endregion

    private val ELF_MAGIC = byteArrayOf(0x7F, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte())
    private const val EI_CLASS = 4
    private const val EI_DATA = 5
    private const val ELFCLASS32 = 1
    private const val ELFCLASS64 = 2
    private const val ELFDATA2LSB = 1
    private const val ELFDATA2MSB = 2

    // the 64-bit header is the bigger one, the 32-bit one fits into it
    private const val ELF64_HEADER_SIZE = 64L

    // section names table is usually smaller than 1 KB, anything bigger is not a sane library
    private const val MAX_NAMES_SECTION_SIZE = 1024L * 1024L

    private val ELF32_LAYOUT = Layout(
        shoffOffset = 0x20,
        shentsizeOffset = 0x2E,
        shnumOffset = 0x30,
        shstrndxOffset = 0x32,
        sectionHeaderSize = 0x28,
        shOffsetOffset = 0x10,
        shSizeOffset = 0x14
    )

    private val ELF64_LAYOUT = Layout(
        shoffOffset = 0x28,
        shentsizeOffset = 0x3A,
        shnumOffset = 0x3C,
        shstrndxOffset = 0x3E,
        sectionHeaderSize = 0x40,
        shOffsetOffset = 0x18,
        shSizeOffset = 0x20
    )

    private const val SH_TYPE_OFFSET = 4
    private const val SHT_SYMTAB = 2
    private const val SHN_UNDEF = 0

    // debug info sections removed by stripping, which are needed to symbolicate a native crash. `.gnu_debugdata`
    // (MiniDebugInfo) is not listed, as symbolic doesn't read it.
    private val DEBUG_INFO_SECTION_NAMES = listOf(".debug_info", ".zdebug_info")
        .map { it.toByteArray(Charsets.US_ASCII) }
}
