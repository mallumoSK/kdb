@file:Suppress("unused")

package tk.mallumo.kdb

@Target(AnnotationTarget.CLASS)
annotation class KdbTable

@Target(AnnotationTarget.CLASS)
annotation class KdbQI

@Target(AnnotationTarget.PROPERTY)
annotation class KdbColumnUnique

@Target(AnnotationTarget.PROPERTY)
annotation class KdbColumnIndex

@Target(AnnotationTarget.PROPERTY)
annotation class KdbColumnSize(val size:Int)