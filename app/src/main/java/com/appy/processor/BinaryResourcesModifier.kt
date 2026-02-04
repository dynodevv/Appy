package com.appy.processor

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Binary resources.arsc modifier
 * 
 * Android's compiled resources.arsc contains a string pool with app strings like the app name.
 * The format is similar to the binary manifest - strings are stored as:
 * - 2 bytes: character count (little-endian, uint16)
 * - N*2 bytes: UTF-16LE encoded characters
 * - 2 bytes: null terminator (0x0000)
 * 
 * This utility finds and replaces string values in the compiled resources.
 */
object BinaryResourcesModifier {
    
    /**
     * Modifies a string value in compiled resources.arsc
     * 
     * @param resourcesBytes The original resources.arsc bytes
     * @param oldValue The string to find (template placeholder)
     * @param newValue The new string to use (user's custom value)
     * @return Modified resources bytes
     * @throws IllegalArgumentException if new value is longer than the template
     */
    fun modifyString(
        resourcesBytes: ByteArray,
        oldValue: String,
        newValue: String
    ): ByteArray {
        if (newValue.length > oldValue.length) {
            throw IllegalArgumentException(
                "Value too long (max ${oldValue.length} chars). Please use a shorter value."
            )
        }
        
        val result = resourcesBytes.copyOf()
        var replaced = false
        
        // resources.arsc uses UTF-16LE for strings
        val oldBytesUtf16 = encodeUtf16Le(oldValue)
        val newBytesUtf16 = encodeUtf16Le(newValue)
        
        var i = 0
        while (i < result.size - oldBytesUtf16.size) {
            if (matchesAt(result, i, oldBytesUtf16)) {
                // Found the old string in UTF-16LE format
                
                // Check if there's a valid length prefix 2 bytes before
                if (i >= 2) {
                    val charCount = (result[i - 2].toInt() and 0xFF) or 
                                   ((result[i - 1].toInt() and 0xFF) shl 8)
                    
                    // Verify this looks like the correct length prefix
                    if (charCount == oldValue.length) {
                        // Update the character count to the new string length
                        result[i - 2] = (newValue.length and 0xFF).toByte()
                        result[i - 1] = ((newValue.length shr 8) and 0xFF).toByte()
                    }
                }
                
                // Write the new string (UTF-16LE encoded)
                for (j in newBytesUtf16.indices) {
                    result[i + j] = newBytesUtf16[j]
                }
                
                // Write null terminator immediately after the new string data
                val nullTermPos = i + newBytesUtf16.size
                if (nullTermPos + 1 < result.size) {
                    result[nullTermPos] = 0
                    result[nullTermPos + 1] = 0
                }
                
                // Clear any remaining bytes from the old string
                for (j in (newBytesUtf16.size + 2) until (oldBytesUtf16.size)) {
                    if (i + j < result.size) {
                        result[i + j] = 0
                    }
                }
                
                replaced = true
                i += oldBytesUtf16.size + 2
            } else {
                i++
            }
        }
        
        if (!replaced) {
            throw IllegalStateException("Could not find template string in resources")
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
