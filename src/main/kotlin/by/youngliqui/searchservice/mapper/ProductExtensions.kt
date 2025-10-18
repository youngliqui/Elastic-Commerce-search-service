package by.youngliqui.searchservice.mapper

import by.youngliqui.searchservice.document.ProductDocument
import by.youngliqui.searchservice.event.dto.ProductResponse

fun ProductResponse.toDocument(): ProductDocument = ProductDocument(
    id = this.id,
    name = this.name,
    description = this.description,
    price = this.price,
    brand = this.brand,
    category = this.category
)