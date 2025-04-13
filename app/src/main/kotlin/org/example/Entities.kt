package org.example.entities

import org.ktorm.entity.Entity
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar
import org.ktorm.schema.datetime
import java.time.LocalDateTime

// Entité User
interface User : Entity<User> {
    companion object : Entity.Factory<User>()
    val idUser: Int
    var userLogin: String
    var userPassword: String
    var userSecretSentence: String
}

// Table Ktorm pour Users
object Users : Table<User>("users") {
    val idUser = int("idUser").primaryKey().bindTo { it.idUser }
    val userLogin = varchar("user_login").bindTo { it.userLogin }
    val userPassword = varchar("user_password").bindTo { it.userPassword }
    val userSecretSentence = varchar("user_secret_sentence").bindTo { it.userSecretSentence }
}

// Entité Color
interface Color : Entity<Color> {
    companion object : Entity.Factory<Color>()
    val idColor: Int
    var colorName: String
    var colorHexa: String
}

// Table Ktorm pour Colors
object Colors : Table<Color>("colors") {
    val idColor = int("idColor").primaryKey().bindTo { it.idColor }
    val colorName = varchar("color_name").bindTo { it.colorName }
    val colorHexa = varchar("color_hexa").bindTo { it.colorHexa }
}

// Entité Note
interface Note : Entity<Note> {
    companion object : Entity.Factory<Note>()
    val idNote: Int
    var noteTitle: String
    var noteContent: String
    var noteCreationDate: LocalDateTime
    var noteUpdateDate: LocalDateTime?
    var noteDeleteDate: LocalDateTime?
    var color: Color
    var user: User
}

// Table Ktorm pour Notes
object Notes : Table<Note>("notes") {
    val idNote = int("idNote").primaryKey().bindTo { it.idNote }
    val noteTitle = varchar("note_title").bindTo { it.noteTitle }
    val noteContent = varchar("note_content").bindTo { it.noteContent }
    val noteCreationDate = datetime("note_creation_date").bindTo { it.noteCreationDate }
    val noteUpdateDate = datetime("note_update_date").bindTo { it.noteUpdateDate }
    val noteDeleteDate = datetime("note_delete_date").bindTo { it.noteDeleteDate }
    val idColor = int("idColor").references(Colors) { it.color }
    val idUser = int("idUser").references(Users) { it.user }
}