/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.utils

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Builds a minimal ELF shared object, containing only the given (empty) sections and the section
 * names table, which is enough to tell if a library is stripped or not.
 */
internal object ElfFileBuilder {

    val UNSTRIPPED_SECTIONS = listOf(".text", ".dynsym", ".symtab", ".strtab", ".debug_info")
    val STRIPPED_SECTIONS = listOf(".text", ".dynsym", ".dynstr")

    fun build(
        sectionNames: List<String>,
        is64Bit: Boolean = true,
        byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN
    ): ByteArray {
        val headerSize = if (is64Bit) 64 else 52
        val sectionHeaderSize = if (is64Bit) 64 else 40

        // section names table: first byte is the empty name of the null section
        val namesTable = ByteArrayOutputStream().apply { write(0) }
        val allSectionNames = sectionNames + ".shstrtab"
        val nameOffsets = allSectionNames.map { name ->
            namesTable.size().also { namesTable.write((name + "\u0000").toByteArray(Charsets.US_ASCII)) }
        }
        val names = namesTable.toByteArray()
        val namesOffset = headerSize
        val sectionHeadersOffset = namesOffset + names.size
        // null section + named sections + names table section
        val sectionsCount = sectionNames.size + 2

        val buffer = ByteBuffer.allocate(sectionHeadersOffset + sectionsCount * sectionHeaderSize)
            .order(byteOrder)
        buffer.put(byteArrayOf(0x7F, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte()))
        buffer.put(if (is64Bit) 2 else 1)
        buffer.put(if (byteOrder == ByteOrder.LITTLE_ENDIAN) 1 else 2)
        buffer.put(1) // EI_VERSION
        buffer.position(16)
        buffer.putShort(3) // e_type = ET_DYN
        buffer.putShort(if (is64Bit) 0xB7 else 0x28) // e_machine = AArch64 / ARM
        buffer.putInt(1) // e_version
        if (is64Bit) {
            buffer.putLong(0) // e_entry
            buffer.putLong(0) // e_phoff
            buffer.putLong(sectionHeadersOffset.toLong()) // e_shoff
        } else {
            buffer.putInt(0)
            buffer.putInt(0)
            buffer.putInt(sectionHeadersOffset)
        }
        buffer.putInt(0) // e_flags
        buffer.putShort(headerSize.toShort()) // e_ehsize
        buffer.putShort(0) // e_phentsize
        buffer.putShort(0) // e_phnum
        buffer.putShort(sectionHeaderSize.toShort()) // e_shentsize
        buffer.putShort(sectionsCount.toShort()) // e_shnum
        buffer.putShort((sectionsCount - 1).toShort()) // e_shstrndx

        buffer.position(namesOffset)
        buffer.put(names)

        // null section header is all zeros
        nameOffsets.forEachIndexed { index, nameOffset ->
            val isNamesTable = index == nameOffsets.lastIndex
            buffer.position(sectionHeadersOffset + (index + 1) * sectionHeaderSize)
            buffer.putInt(nameOffset) // sh_name
            buffer.putInt(sectionType(allSectionNames[index])) // sh_type
            val offset = if (isNamesTable) namesOffset else 0
            val size = if (isNamesTable) names.size else 0
            if (is64Bit) {
                buffer.putLong(0) // sh_flags
                buffer.putLong(0) // sh_addr
                buffer.putLong(offset.toLong()) // sh_offset
                buffer.putLong(size.toLong()) // sh_size
            } else {
                buffer.putInt(0)
                buffer.putInt(0)
                buffer.putInt(offset)
                buffer.putInt(size)
            }
        }

        return buffer.array()
    }

    private fun sectionType(name: String): Int = when (name) {
        ".symtab" -> 2 // SHT_SYMTAB
        ".strtab", ".dynstr", ".shstrtab" -> 3 // SHT_STRTAB
        ".dynsym" -> 11 // SHT_DYNSYM
        else -> 1 // SHT_PROGBITS
    }
}
