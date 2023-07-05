import org.springframework.web.multipart.MultipartFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.reflect.Field
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

object CsvValidator {
    fun <T : Any> validateCsvFile(file: MultipartFile, entityClass: Class<T>, ignoredFields: List<String> = emptyList()): List<T> {
        val reader = BufferedReader(InputStreamReader(file.inputStream))
        // CSV 파일이 비어있는지 확인
        val header: Array<out String> = reader.readLine()?.split(",")?.toTypedArray()
                ?: throw Exception("CSV file is empty")
        println(header)
        // Class의 모든 필드명 가져옴
        val fields = entityClass.kotlin.memberProperties.mapNotNull { it.javaField }.toList()

        // csv 파일의 컬럼명이랑 entity 필드명 같은지다른지 확인
        if (header.size != fields.size - ignoredFields.size) {
            throw Exception("Number of columns in CSV file doesn't match number of fields in entity class")
        }

        for (i in header.indices) {
            if (fields.find { it.name == header[i] } == null) {
                throw Exception("CSV file's column names are not in the entity class's field names")
            }
        }

        var line: Array<String>?
        val resultEntityList = mutableListOf<T>()
        while (reader.readLine().also { line = it?.split(",")?.toTypedArray() } != null) {
            val entityInstance = entityClass.getDeclaredConstructor().newInstance()

            if (line!!.size != fields.size - ignoredFields.size) {
                throw Exception("Number of values in a row doesn't match number of fields in entity class")
            }

            for (i in line!!.indices) {
                val fieldName = header[i]
                val field: Field? = fields.find { it.name == fieldName }

                if (field == null || ignoredFields.contains(fieldName)) {
                    continue
                }

                field.trySetAccessible()
                try {
                    val value = line!![i]
                    when (field.type) {
                        String::class.java -> field.set(entityInstance, value)
                        Long::class.java -> field.set(entityInstance, value.toLong())
                        Float::class.java -> field.set(entityInstance, value.toFloat())
                        Double::class.java -> field.set(entityInstance, value.toDouble())
                        Int::class.java -> field.set(entityInstance, value.toInt())
                        Char::class.java -> field.set(entityInstance, value[0])
                        Boolean::class.java -> field.set(entityInstance, value.toBoolean())
                        Short::class.java -> field.set(entityInstance, value.toShort())
                        Byte::class.java -> field.set(entityInstance, value.toByte())
                        else -> throw Exception("CSV file value type doesn't match entity class's field type")
                    }
                } catch (e: NumberFormatException) {
                    throw Exception("CSV file value type doesn't match entity class field type", e)
                }
            }
            resultEntityList.add(entityInstance)
        }

        return resultEntityList
    }

}