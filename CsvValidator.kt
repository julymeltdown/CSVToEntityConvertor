
import com.gausslab.weatherapi.input.exception.FieldTypeNotSameWithColumnException
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
                ?: throw Exception()
        println(header)
        // Class의 모든 필드명 가져옴
        val fields = entityClass.kotlin.memberProperties.mapNotNull { it.javaField }.toList()

        // 컬럼 개수 안맞음
        if (header.size != fields.size - ignoredFields.size) {
            throw Exception()
        }
        // csv 파일의 컬럼명이랑 entity 필드명 같은지다른지 확인
        for (i in header.indices) {
            if (fields.find { it.name == header[i] } == null) {
                throw Exception()
            }
        }

        var line: Array<String>?
        val resultEntityList = mutableListOf<T>()
        while (reader.readLine().also { line = it?.split(",")?.toTypedArray() } != null) {
            val entityInstance = entityClass.getDeclaredConstructor().newInstance()

            for (i in line!!.indices) {
                val fieldName = header[i]
                val field: Field? = fields.find { it.name == fieldName }

                if (field == null || ignoredFields.contains(fieldName)) {
                    continue
                }
                field.trySetAccessible()
                //여기가 고쳐져야하는 이유
                // 지금 csv 파일의 각각의 값이 어떤 데이터 타입으로넘어올지가 정해지지 않음
                // -> fdsafds이런걸 입력해도 일단은 DB에는 String으로 저장하고있어 정상 저장됨
                // 파일의 자료형이 어떤 데이터 타입이 될지를 요청해서 알아야함...................
                try {
                    val value = line!![i]
                    when (field.type) {

                        String::class.java -> field.set(entityInstance, value)
                        Long::class.java -> field.set(entityInstance, value.toLongOrNull())
                        Float::class.java -> field.set(entityInstance, value.toFloatOrNull())
                        Double::class.java -> field.set(entityInstance, value.toDoubleOrNull())
                        Int::class.java -> field.set(entityInstance, value.toIntOrNull())
                        Char::class.java -> field.set(entityInstance, value[0])
                        Boolean::class.java -> field.set(entityInstance, value.toBooleanStrictOrNull())
                        Short::class.java -> field.set(entityInstance, value.toShortOrNull())
                        Byte::class.java -> field.set(entityInstance, value.toByteOrNull())
                        else -> throw Exception()
                    }
                } catch (e: Exception) {
                    throw FieldTypeNotSameWithColumnException()
                }
            }
            resultEntityList.add(entityInstance)
        }

        return resultEntityList
    }

}