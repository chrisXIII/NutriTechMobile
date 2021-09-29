package com.istea.nutritechmobile.data

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp
import java.io.Serializable

data class User(
    val mail: String,
    val password: String,
) : Serializable

data class UserResponse(
    var Nombre: String,
    var Apellido: String,
    var Email: String,
    var Password: String,
    var Rol: String,
    var FechaNacimiento: Timestamp?,
    var Telefono: String,
    var LastUpdated: Timestamp?,
    var Altura: Float?,
    var Peso: Float?,
    var MedidaCintura: Float?,
    var TipoAlimentacion: String,
    var PlanAsignado: PlanAsignacion?,
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readParcelable(Timestamp::class.java.classLoader),
        parcel.readString() ?: "",
        parcel.readParcelable(Timestamp::class.java.classLoader),
        parcel.readValue(Float::class.java.classLoader) as? Float,
        parcel.readValue(Float::class.java.classLoader) as? Float,
        parcel.readValue(Float::class.java.classLoader) as? Float,
        parcel.readString() ?: "",
        parcel.readParcelable(PlanAsignacion::class.java.classLoader) as PlanAsignacion?
    ) {
    }

    constructor() : this(
        "",
        "",
        "",
        "",
        "",
        Timestamp.now(),
        "",
        Timestamp.now(),
        0f,
        0f,
        0f,
        "",
        PlanAsignacion(
            "",
            "",
            Timestamp.now()
        )
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(Nombre)
        parcel.writeString(Apellido)
        parcel.writeString(Email)
        parcel.writeString(Password)
        parcel.writeString(Rol)
        parcel.writeParcelable(FechaNacimiento, flags)
        parcel.writeString(Telefono)
        parcel.writeParcelable(LastUpdated, flags)
        parcel.writeValue(Altura)
        parcel.writeValue(Peso)
        parcel.writeValue(MedidaCintura)
        parcel.writeString(TipoAlimentacion)
        parcel.writeParcelable(PlanAsignado, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<UserResponse> {
        override fun createFromParcel(parcel: Parcel): UserResponse {
            return UserResponse(parcel)
        }

        override fun newArray(size: Int): Array<UserResponse?> {
            return arrayOfNulls(size)
        }
    }
}