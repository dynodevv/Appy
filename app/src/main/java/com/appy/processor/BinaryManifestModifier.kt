package com.appy.processor

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Binary AndroidManifest.xml modifier
 * 
 * Android's compiled AndroidManifest.xml uses a binary format where:
 * - Strings are stored in a string pool at the beginning
 * - The package name is one of these strings
 * - We can find and replace the package name string with a new one
 * 
 * This implementation performs direct byte replacement, which works when:
 * - The new package name is the same length or shorter than the template
 * - The template package name is long enough to accommodate custom names
 */
object BinaryManifestModifier {
    
    /**
     * Modifies the package name in a binary AndroidManifest.xml
     * 
     * @param manifestBytes The original binary manifest bytes
     * @param oldPackageName The package name to find (template package ID)
     * @param newPackageName The new package name to use (user's custom package ID)
     * @return Modified manifest bytes
     * @throws IllegalArgumentException if new package name is longer than the template
     */
    fun modifyPackageName(
        manifestBytes: ByteArray,
        oldPackageName: String,
        newPackageName: String
    ): ByteArray {
        if (newPackageName.length > oldPackageName.length) {
            throw IllegalArgumentException(
                "Package ID too long (max ${oldPackageName.length} chars). Please use a shorter package ID."
            )
        }
        
        // Simple replacement: find the old package name and replace with new one (padded)
        val oldBytes = oldPackageName.toByteArray(Charsets.UTF_8)
        val newBytes = newPackageName.toByteArray(Charsets.UTF_8)
        
        val result = manifestBytes.copyOf()
        var replacedUtf8 = false
        var replacedUtf16 = false
        
        // Find and replace UTF-8 occurrences
        var i = 0
        while (i < result.size - oldBytes.size) {
            if (matchesAt(result, i, oldBytes)) {
                // Replace with new bytes, pad with zeros if necessary
                for (j in newBytes.indices) {
                    result[i + j] = newBytes[j]
                }
                // Pad remaining bytes with zeros
                for (j in newBytes.size until oldBytes.size) {
                    result[i + j] = 0
                }
                replacedUtf8 = true
                i += oldBytes.size
            } else {
                i++
            }
        }
        
        // Also handle UTF-16 encoded strings (Android binary XML uses UTF-16 for strings)
        val oldBytesUtf16 = encodeUtf16Le(oldPackageName)
        val newBytesUtf16 = encodeUtf16Le(newPackageName)
        
        i = 0
        while (i < result.size - oldBytesUtf16.size) {
            if (matchesAt(result, i, oldBytesUtf16)) {
                // Replace with new bytes, pad with zeros if necessary
                for (j in newBytesUtf16.indices) {
                    result[i + j] = newBytesUtf16[j]
                }
                // Pad remaining bytes with zeros (in pairs for UTF-16)
                for (j in newBytesUtf16.size until oldBytesUtf16.size) {
                    result[i + j] = 0
                }
                replacedUtf16 = true
                i += oldBytesUtf16.size
            } else {
                i++
            }
        }
        
        if (!replacedUtf8 && !replacedUtf16) {
            throw IllegalStateException("Could not find template package ID in manifest")
        }
        
        return result
    }
    
    /**
     * Check if bytes at position match the target bytes
     */
    private fun matchesAt(data: ByteArray, position: Int, target: ByteArray): Boolean {
        if (position + target.size > data.size) return false
        for (i in target.indices) {
            if (data[position + i] != target[i]) return false
        }
        return true
    }
    
    /**
     * Encode string as UTF-16LE (little endian, no BOM)
     */
    private fun encodeUtf16Le(str: String): ByteArray {
        val buffer = ByteBuffer.allocate(str.length * 2)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        for (char in str) {
            buffer.putChar(char)
        }
        return buffer.array()
    }
}
