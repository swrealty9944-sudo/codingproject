package com.macro.app.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ImageRepository(private val context: Context) {
    
    private val templatesDir: File
        get() = File(context.filesDir, "templates").apply {
            if (!exists()) mkdirs()
        }
    
    data class TemplateInfo(
        val id: String,
        val name: String,
        val filePath: String,
        val width: Int,
        val height: Int,
        val createdAt: Long
    )
    
    fun saveTemplate(bitmap: Bitmap, name: String): String {
        val templateId = UUID.randomUUID().toString()
        val file = File(templatesDir, "$templateId.png")
        
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        
        saveTemplateInfo(TemplateInfo(
            id = templateId,
            name = name,
            filePath = file.absolutePath,
            width = bitmap.width,
            height = bitmap.height,
            createdAt = System.currentTimeMillis()
        ))
        
        return templateId
    }
    
    fun loadTemplate(templateId: String): Bitmap? {
        val file = File(templatesDir, "$templateId.png")
        return if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            null
        }
    }
    
    fun deleteTemplate(templateId: String): Boolean {
        val file = File(templatesDir, "$templateId.png")
        val infoFile = File(templatesDir, "$templateId.json")
        
        val fileDeleted = if (file.exists()) file.delete() else true
        val infoDeleted = if (infoFile.exists()) infoFile.delete() else true
        
        return fileDeleted && infoDeleted
    }
    
    fun listTemplates(): List<TemplateInfo> {
        val templates = mutableListOf<TemplateInfo>()
        
        templatesDir.listFiles { file -> 
            file.extension == "png" 
        }?.forEach { file ->
            val templateId = file.nameWithoutExtension
            loadTemplateInfo(templateId)?.let { info ->
                templates.add(info)
            }
        }
        
        return templates.sortedByDescending { it.createdAt }
    }
    
    fun getTemplateInfo(templateId: String): TemplateInfo? {
        return loadTemplateInfo(templateId)
    }
    
    private fun saveTemplateInfo(info: TemplateInfo) {
        val infoFile = File(templatesDir, "${info.id}.json")
        val json = """
            {
                "id": "${info.id}",
                "name": "${info.name}",
                "filePath": "${info.filePath}",
                "width": ${info.width},
                "height": ${info.height},
                "createdAt": ${info.createdAt}
            }
        """.trimIndent()
        
        infoFile.writeText(json)
    }
    
    private fun loadTemplateInfo(templateId: String): TemplateInfo? {
        val infoFile = File(templatesDir, "$templateId.json")
        if (!infoFile.exists()) {
            val imageFile = File(templatesDir, "$templateId.png")
            if (!imageFile.exists()) return null
            
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath) ?: return null
            return TemplateInfo(
                id = templateId,
                name = templateId,
                filePath = imageFile.absolutePath,
                width = bitmap.width,
                height = bitmap.height,
                createdAt = imageFile.lastModified()
            )
        }
        
        return try {
            val json = infoFile.readText()
            parseTemplateInfo(json)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseTemplateInfo(json: String): TemplateInfo? {
        return try {
            val idMatch = Regex(""""id":\s*"([^"]+)"""").find(json)
            val nameMatch = Regex(""""name":\s*"([^"]+)"""").find(json)
            val pathMatch = Regex(""""filePath":\s*"([^"]+)"""").find(json)
            val widthMatch = Regex(""""width":\s*(\d+)""").find(json)
            val heightMatch = Regex(""""height":\s*(\d+)""").find(json)
            val createdMatch = Regex(""""createdAt":\s*(\d+)""").find(json)
            
            TemplateInfo(
                id = idMatch?.groupValues?.get(1) ?: return null,
                name = nameMatch?.groupValues?.get(1) ?: return null,
                filePath = pathMatch?.groupValues?.get(1) ?: return null,
                width = widthMatch?.groupValues?.get(1)?.toInt() ?: return null,
                height = heightMatch?.groupValues?.get(1)?.toInt() ?: return null,
                createdAt = createdMatch?.groupValues?.get(1)?.toLong() ?: return null
            )
        } catch (e: Exception) {
            null
        }
    }
}
