import com.google.android.material.datepicker.CalendarConstraints
import android.os.Parcel
import android.os.Parcelable
import java.util.Calendar

class DayOfWeekValidator(private val allowedDays: Set<Int>) : CalendarConstraints.DateValidator {

    override fun isValid(date: Long): Boolean {
        val cal = Calendar.getInstance()
        cal.timeInMillis = date
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        return allowedDays.contains(dayOfWeek)
    }

    override fun describeContents(): Int = 0

    // ✅ Correct function signature
    override fun writeToParcel(dest: Parcel, flags: Int) {
        val daysArray = allowedDays.toIntArray()
        dest.writeInt(daysArray.size)
        daysArray.forEach { dest.writeInt(it) }
    }

    companion object CREATOR : Parcelable.Creator<DayOfWeekValidator> {
        override fun createFromParcel(parcel: Parcel): DayOfWeekValidator {
            val size = parcel.readInt()
            val days = mutableSetOf<Int>()
            repeat(size) {
                days.add(parcel.readInt())
            }
            return DayOfWeekValidator(days)
        }

        override fun newArray(size: Int): Array<DayOfWeekValidator?> = arrayOfNulls(size)
    }
}
