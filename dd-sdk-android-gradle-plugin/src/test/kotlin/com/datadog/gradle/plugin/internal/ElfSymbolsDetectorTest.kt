/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal

import com.datadog.gradle.plugin.utils.ElfFileBuilder
import com.datadog.gradle.plugin.utils.forge.Configurator
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

@Extensions(
    ExtendWith(ForgeExtension::class)
)
@ForgeConfiguration(Configurator::class)
internal class ElfSymbolsDetectorTest {

    @TempDir
    lateinit var tempDir: File

    @ParameterizedTest
    @MethodSource("elfFormats")
    fun `M return HAS_SYMBOLS W detect() { library has symbol table }`(is64Bit: Boolean, byteOrder: ByteOrder) {
        // Given
        val file = writeFile(ElfFileBuilder.build(listOf(".text", ".symtab", ".strtab"), is64Bit, byteOrder))

        // When
        val result = ElfSymbolsDetector.detect(file)

        // Then
        assertThat(result).isEqualTo(ElfSymbolsDetector.Result.HAS_SYMBOLS)
    }

    @ParameterizedTest
    @MethodSource("elfFormats")
    fun `M return HAS_SYMBOLS W detect() { library has debug info only }`(is64Bit: Boolean, byteOrder: ByteOrder) {
        // Given
        val file = writeFile(ElfFileBuilder.build(listOf(".text", ".debug_info"), is64Bit, byteOrder))

        // When
        val result = ElfSymbolsDetector.detect(file)

        // Then
        assertThat(result).isEqualTo(ElfSymbolsDetector.Result.HAS_SYMBOLS)
    }

    @ParameterizedTest
    @MethodSource("elfFormats")
    fun `M return STRIPPED W detect() { library has no symbols }`(is64Bit: Boolean, byteOrder: ByteOrder) {
        // Given
        val file = writeFile(ElfFileBuilder.build(ElfFileBuilder.STRIPPED_SECTIONS, is64Bit, byteOrder))

        // When
        val result = ElfSymbolsDetector.detect(file)

        // Then
        assertThat(result).isEqualTo(ElfSymbolsDetector.Result.STRIPPED)
    }

    @Test
    fun `M return UNKNOWN W detect() { not an ELF file }`(
        @StringForgery fakeContent: String
    ) {
        // Given
        val file = writeFile(fakeContent.toByteArray())

        // When
        val result = ElfSymbolsDetector.detect(file)

        // Then
        assertThat(result).isEqualTo(ElfSymbolsDetector.Result.UNKNOWN)
    }

    @Test
    fun `M return UNKNOWN W detect() { empty file }`() {
        // Given
        val file = writeFile(ByteArray(0))

        // When
        val result = ElfSymbolsDetector.detect(file)

        // Then
        assertThat(result).isEqualTo(ElfSymbolsDetector.Result.UNKNOWN)
    }

    @Test
    fun `M return UNKNOWN W detect() { truncated ELF file }`(forge: Forge) {
        // Given
        val content = ElfFileBuilder.build(ElfFileBuilder.UNSTRIPPED_SECTIONS)
        val file = writeFile(content.copyOf(forge.anInt(min = 4, max = content.size)))

        // When
        val result = ElfSymbolsDetector.detect(file)

        // Then
        assertThat(result).isEqualTo(ElfSymbolsDetector.Result.UNKNOWN)
    }

    @Test
    fun `M return UNKNOWN W detect() { unexpected section header size }`(forge: Forge) {
        // Given
        val content = ElfFileBuilder.build(ElfFileBuilder.UNSTRIPPED_SECTIONS)
        val buffer = ByteBuffer.wrap(content).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(ELF64_SHENTSIZE_OFFSET, forge.anElementFrom(40, 63, 65, 128).toShort())
        val file = writeFile(content)

        // When
        val result = ElfSymbolsDetector.detect(file)

        // Then
        assertThat(result).isEqualTo(ElfSymbolsDetector.Result.UNKNOWN)
    }

    @Test
    fun `M return UNKNOWN W detect() { section names table is too big }`() {
        // Given
        // the names table declares a size bigger than the limit, but which still fits into the file.
        // No symbol table, so that the names table has to be read to look for debug info.
        val content = ElfFileBuilder.build(listOf(".text", ".debug_info"))
        val buffer = ByteBuffer.wrap(content).order(ByteOrder.LITTLE_ENDIAN)
        val sectionHeadersOffset = buffer.getLong(ELF64_SHOFF_OFFSET).toInt()
        val namesSectionIndex = buffer.getShort(ELF64_SHSTRNDX_OFFSET).toInt()
        val namesSizeOffset = sectionHeadersOffset + namesSectionIndex * ELF64_SECTION_HEADER_SIZE + 0x20
        val namesSize = 2L * 1024L * 1024L
        buffer.putLong(namesSizeOffset, namesSize)
        val file = writeFile(content.copyOf(content.size + namesSize.toInt()))

        // When
        val result = ElfSymbolsDetector.detect(file)

        // Then
        assertThat(result).isEqualTo(ElfSymbolsDetector.Result.UNKNOWN)
    }

    @Test
    fun `M return HAS_SYMBOLS W detect() { symbol table without section names table }`() {
        // Given
        val content = ElfFileBuilder.build(listOf(".text", ".symtab", ".strtab"))
        ByteBuffer.wrap(content).order(ByteOrder.LITTLE_ENDIAN).putShort(ELF64_SHSTRNDX_OFFSET, 0)
        val file = writeFile(content)

        // When
        val result = ElfSymbolsDetector.detect(file)

        // Then
        assertThat(result).isEqualTo(ElfSymbolsDetector.Result.HAS_SYMBOLS)
    }

    @Test
    fun `M return UNKNOWN W detect() { no symbol table and no section names table }`() {
        // Given
        // debug info can't be identified without the section names
        val content = ElfFileBuilder.build(listOf(".text", ".debug_info"))
        ByteBuffer.wrap(content).order(ByteOrder.LITTLE_ENDIAN).putShort(ELF64_SHSTRNDX_OFFSET, 0)
        val file = writeFile(content)

        // When
        val result = ElfSymbolsDetector.detect(file)

        // Then
        assertThat(result).isEqualTo(ElfSymbolsDetector.Result.UNKNOWN)
    }

    @Test
    fun `M return STRIPPED W detect() { no section header table }`() {
        // Given
        // as produced by `strip --strip-section-headers`
        val content = ElfFileBuilder.build(ElfFileBuilder.UNSTRIPPED_SECTIONS)
        val buffer = ByteBuffer.wrap(content).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putLong(ELF64_SHOFF_OFFSET, 0)
        buffer.putShort(ELF64_SHNUM_OFFSET, 0)
        buffer.putShort(ELF64_SHSTRNDX_OFFSET, 0)
        val file = writeFile(content)

        // When
        val result = ElfSymbolsDetector.detect(file)

        // Then
        assertThat(result).isEqualTo(ElfSymbolsDetector.Result.STRIPPED)
    }

    @Test
    fun `M return UNKNOWN W detect() { extended section numbering }`() {
        // Given
        // the section header table is there, but its size is stored in the first section header
        val content = ElfFileBuilder.build(ElfFileBuilder.UNSTRIPPED_SECTIONS)
        ByteBuffer.wrap(content).order(ByteOrder.LITTLE_ENDIAN).putShort(ELF64_SHNUM_OFFSET, 0)
        val file = writeFile(content)

        // When
        val result = ElfSymbolsDetector.detect(file)

        // Then
        assertThat(result).isEqualTo(ElfSymbolsDetector.Result.UNKNOWN)
    }

    @Test
    fun `M return STRIPPED W detect() { only MiniDebugInfo }`() {
        // Given
        // symbolic doesn't read .gnu_debugdata, so the library is as useful as any stripped one
        val file = writeFile(ElfFileBuilder.build(ElfFileBuilder.STRIPPED_SECTIONS + ".gnu_debugdata"))

        // When
        val result = ElfSymbolsDetector.detect(file)

        // Then
        assertThat(result).isEqualTo(ElfSymbolsDetector.Result.STRIPPED)
    }

    @Test
    fun `M return STRIPPED W detect() { section named symtab with another type }`() {
        // Given
        // symbolic only reads the symbol table from a section of type SHT_SYMTAB
        val content = ElfFileBuilder.build(listOf(".text", ".symtab", ".dynsym"))
        val buffer = ByteBuffer.wrap(content).order(ByteOrder.LITTLE_ENDIAN)
        val sectionHeadersOffset = buffer.getLong(ELF64_SHOFF_OFFSET).toInt()
        // sections: null, .text, .symtab, ...
        buffer.putInt(sectionHeadersOffset + 2 * ELF64_SECTION_HEADER_SIZE + 4, 1)
        val file = writeFile(content)

        // When
        val result = ElfSymbolsDetector.detect(file)

        // Then
        assertThat(result).isEqualTo(ElfSymbolsDetector.Result.STRIPPED)
    }

    @Test
    @Timeout(10, unit = TimeUnit.SECONDS)
    fun `M return STRIPPED quickly W detect() { many sections share a long unterminated name }`() {
        // Given
        // every section points at the same name, which runs until the end of the names table
        val sectionsCount = 60_000
        val content = ElfFileBuilder.build(List(sectionsCount) { ".text" })
        val buffer = ByteBuffer.wrap(content).order(ByteOrder.LITTLE_ENDIAN)
        val sectionHeadersOffset = buffer.getLong(ELF64_SHOFF_OFFSET).toInt()
        val namesSectionIndex = buffer.getShort(ELF64_SHSTRNDX_OFFSET).toInt() and 0xFFFF
        val namesHeaderOffset = sectionHeadersOffset + namesSectionIndex * ELF64_SECTION_HEADER_SIZE
        val namesOffset = buffer.getLong(namesHeaderOffset + 0x18).toInt()
        val namesSize = buffer.getLong(namesHeaderOffset + 0x20).toInt()
        content.fill('a'.code.toByte(), namesOffset + 1, namesOffset + namesSize)
        for (index in 1..namesSectionIndex) {
            buffer.putInt(sectionHeadersOffset + index * ELF64_SECTION_HEADER_SIZE, 1)
        }
        val file = writeFile(content)

        // When
        val result = ElfSymbolsDetector.detect(file)

        // Then
        assertThat(result).isEqualTo(ElfSymbolsDetector.Result.STRIPPED)
    }

    @Test
    fun `M return UNKNOWN W detect() { file doesn't exist }`() {
        // When
        val result = ElfSymbolsDetector.detect(File(tempDir, "missing.so"))

        // Then
        assertThat(result).isEqualTo(ElfSymbolsDetector.Result.UNKNOWN)
    }

    private fun writeFile(content: ByteArray): File {
        return File(tempDir, "libfake.so").apply { writeBytes(content) }
    }

    companion object {
        private const val ELF64_SHOFF_OFFSET = 0x28
        private const val ELF64_SHENTSIZE_OFFSET = 0x3A
        private const val ELF64_SHNUM_OFFSET = 0x3C
        private const val ELF64_SHSTRNDX_OFFSET = 0x3E
        private const val ELF64_SECTION_HEADER_SIZE = 64

        @JvmStatic
        fun elfFormats() = listOf(true, false).flatMap { is64Bit ->
            listOf(ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN).map { Arguments.of(is64Bit, it) }
        }
    }
}
