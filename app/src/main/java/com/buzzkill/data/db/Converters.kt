package com.buzzkill.data.db

import androidx.room.TypeConverter
import com.buzzkill.data.model.Action
import com.buzzkill.data.model.Condition
import com.buzzkill.data.model.LogicMode
import com.buzzkill.data.model.Trigger
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Shared lenient JSON used for both persistence and import/export. */
val BuzzJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    classDiscriminator = "type"
    prettyPrint = false
}

/** Room converters that store rule component lists as JSON text columns. */
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String =
        BuzzJson.encodeToString(ListSerializer(String.serializer()), value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        BuzzJson.decodeFromString(ListSerializer(String.serializer()), value)

    @TypeConverter
    fun fromTriggers(value: List<Trigger>): String =
        BuzzJson.encodeToString(ListSerializer(Trigger.serializer()), value)

    @TypeConverter
    fun toTriggers(value: String): List<Trigger> =
        BuzzJson.decodeFromString(ListSerializer(Trigger.serializer()), value)

    @TypeConverter
    fun fromConditions(value: List<Condition>): String =
        BuzzJson.encodeToString(ListSerializer(Condition.serializer()), value)

    @TypeConverter
    fun toConditions(value: String): List<Condition> =
        BuzzJson.decodeFromString(ListSerializer(Condition.serializer()), value)

    @TypeConverter
    fun fromActions(value: List<Action>): String =
        BuzzJson.encodeToString(ListSerializer(Action.serializer()), value)

    @TypeConverter
    fun toActions(value: String): List<Action> =
        BuzzJson.decodeFromString(ListSerializer(Action.serializer()), value)

    @TypeConverter
    fun fromLogicMode(value: LogicMode): String = value.name

    @TypeConverter
    fun toLogicMode(value: String): LogicMode = LogicMode.valueOf(value)
}
