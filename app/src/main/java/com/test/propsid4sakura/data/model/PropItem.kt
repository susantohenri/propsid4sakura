package com.test.propsid4sakura.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a single prop item from content.json.
 * Remote format: flat array [ { "category":..., "title":..., "prop_id":... } ]
 */
@Serializable
data class PropItem(
    @SerialName("category") val category: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("prop_id") val propId: String = ""
)
