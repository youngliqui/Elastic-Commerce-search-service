package by.youngliqui.searchservice.document

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.util.UUID

@Document(indexName = "products")
data class ProductDocument(
    @Id
    val id: UUID,

    @Field(type = FieldType.Text, analyzer = "standard")
    val name: String,

    @Field(type = FieldType.Text, analyzer = "standard")
    val description: String?,

    @Field(type = FieldType.Keyword)
    val brand: String,

    @Field(type = FieldType.Keyword)
    val category: String,

    @Field(type = FieldType.Double)
    val price: Double
)