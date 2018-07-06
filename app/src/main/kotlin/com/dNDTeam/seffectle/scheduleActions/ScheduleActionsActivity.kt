package com.dNDTeam.seffectle.scheduleActions

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import com.dNDTeam.seffectle.R
import com.dNDTeam.seffectle.db.ClassFromScheduleParser
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.KEY_CLASS_BEGIN_TIME
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.KEY_CLASS_END_TIME
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.KEY_CLASS_NAME
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.KEY_CLASS_SERIAL_NUMBER
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.KEY_CLASS_TYPE
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.KEY_DAY_OF_WEEK
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.KEY_ROOM_NAME
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.KEY_SCHEDULE_CREATION_TIME
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.KEY_TEACHER_NAME
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.SCHEDULE_TABLE_NAME
import com.dNDTeam.seffectle.db.database
import kotlinx.android.synthetic.main.activity_schedule_actions.recyclerView_SA
import kotlinx.android.synthetic.main.activity_schedule_actions.toolbar_AM
import org.jetbrains.anko.db.delete
import org.jetbrains.anko.db.insert
import org.jetbrains.anko.db.select
import org.jetbrains.anko.longToast
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.regex.Pattern


//https://www.androidhive.info/2015/09/android-material-design-floating-labels-for-edittext/
class ScheduleActionsActivity : AppCompatActivity() {
    //для какого дня создаётся расписание
    private val selectedDay by lazy(LazyThreadSafetyMode.NONE) {
        intent.getIntExtra("selectedDay", 0)
    }

    private fun translateTimeToMinutes(time: List<String>) =
            time[0].toInt() * 60 + time[1].toInt()

    //список пар, считанных с БД
    private val classesFromDB by lazy(LazyThreadSafetyMode.NONE) {
        database.use {
            select(SCHEDULE_TABLE_NAME)
                    //метод whereArgs защищает (судя по исходникам) от SQL-инъекций
                    .whereArgs("($KEY_DAY_OF_WEEK = {$KEY_DAY_OF_WEEK})", KEY_DAY_OF_WEEK to selectedDay)
                    .parseList(ClassFromScheduleParser)
        }.toMutableList()
    }

    private val scheduleAdapter by lazy(LazyThreadSafetyMode.NONE) {
        ScheduleAdapter(mutableListOf(*classesFromDB.map { it to false }.toTypedArray()), this@ScheduleActionsActivity, selectedDay)
    }

    private fun RecyclerView.setup() {
        adapter = this@ScheduleActionsActivity.scheduleAdapter
        layoutManager = LinearLayoutManager(
                this@ScheduleActionsActivity,
                LinearLayoutManager.VERTICAL,
                false
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_actions)

        setSupportActionBar(toolbar_AM)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
            title = resources.getStringArray(R.array.days_of_week)[selectedDay]
        }

        recyclerView_SA.setup()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_schedule_actions, menu)
        return true
    }

    private fun getCurrentDate() =
    //неплохая статья - https://www.programiz.com/kotlin-programming/examples/current-date-time
    //получаем текущую дату для уникальной идентификации времени, когда было создано расписание
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            //лучше использовать этот код, чем тот, который в else,
            //но увы Java 8 только с Android 8.0(Oreo)
                LocalDateTime.now().toString()
            else
                GregorianCalendar().time.toString()


    @SuppressLint("SetTextI18n")
    override fun onOptionsItemSelected(item: MenuItem) =
            when (item.itemId) {
                R.id.action_add_class -> {
                    if (scheduleAdapter.itemCount != 7) {
                        scheduleAdapter.addNewItem()
                        scheduleAdapter.hideAllItemsExcludeAtAndScrollTo(scheduleAdapter.itemCount - 1)
                    } else
                        longToast(getString(R.string.cant_add_class))

                    true
                }
                R.id.action_delete_schedule -> {
                    scheduleAdapter.clear()

                    true
                }
                R.id.action_save_schedule -> {
                    var tempClassesList = mutableListOf<Array<Pair<String, String>>>()
                    val timePattern = Pattern.compile("^(([0,1][0-9])|(2[0-3])):[0-5][0-9]$")
                    var haveError = false
                    //неплохая статья - https://www.programiz.com/kotlin-programming/examples/current-date-time
                    val currentDate = getCurrentDate()

                    for (i in 0 until scheduleAdapter.itemCount) {
                        val classData = scheduleAdapter.getItem(i)
                        val classBeginTime = classData.classBeginTime

                        //Проверка данных
                        if (!timePattern.matcher(classBeginTime).matches()) {
                            longToast(getString(R.string.format_begin_time_error))
                            haveError = true
                            break
                        }

                        val classEndTime = classData.classEndTime

                        //Проверка данных
                        if (!timePattern.matcher(classEndTime).matches()) {
                            longToast(getString(R.string.format_end_time_error))
                            haveError = true
                            break
                        }

                        //Проверка непересекаемости интервалов времени
                        val classBeginTimeInMinutes = translateTimeToMinutes(classBeginTime.split(":"))
                        val classEndTimeInMinutes = translateTimeToMinutes(classEndTime.split(":"))

                        if (classEndTimeInMinutes <= classBeginTimeInMinutes) {
                            longToast(getString(R.string.wrong_time_interval))
                            haveError = true
                            break
                        }

                        tempClassesList.add(arrayOf(
                                KEY_DAY_OF_WEEK to selectedDay.toString(),
                                KEY_CLASS_BEGIN_TIME to classBeginTime,
                                KEY_CLASS_END_TIME to classEndTime,
                                KEY_CLASS_TYPE to classData.classType,
                                KEY_CLASS_NAME to classData.className,
                                KEY_TEACHER_NAME to classData.teacherName,
                                KEY_ROOM_NAME to classData.roomName,
                                KEY_CLASS_SERIAL_NUMBER to classData.classSerialNumber,
                                KEY_SCHEDULE_CREATION_TIME to currentDate))
                    }

                    //сортируем по времени
                    tempClassesList.sortBy { it[1].second }

                    val times = tempClassesList.map { it[1].second to it[2].second }

                    //проверяем на пересечение времени пар
                    for (index in 0 until times.size - 1) {
                        val firstClass = times[index]
                        val secondClass = times[index + 1]

                        val startHoursAndMinutes = firstClass.first.split(":")
                        val startHours = startHoursAndMinutes[0].toInt()
                        val startMinutes = startHoursAndMinutes[1].toInt()

                        val endHoursAndMinutes = firstClass.second.split(":")
                        val endHours = endHoursAndMinutes[0].toInt()
                        val endMinutes = endHoursAndMinutes[1].toInt()

                        val startHoursAndMinutes2 = secondClass.first.split(":")
                        val startHours2 = startHoursAndMinutes2[0].toInt()
                        val startMinutes2 = startHoursAndMinutes2[1].toInt()

                        val endHoursAndMinutes2 = secondClass.second.split(":")
                        val endHours2 = endHoursAndMinutes2[0].toInt()
                        val endMinutes2 = endHoursAndMinutes2[1].toInt()

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            if (isOverlappingJava8(
                                            getDateJava8(startHours, startMinutes),
                                            getDateJava8(endHours, endMinutes),
                                            getDateJava8(startHours2, startMinutes2),
                                            getDateJava8(endHours2, endMinutes2)
                                    )) {
                                longToast(getString(R.string.time_of_classes_must_not_intersect))
                                haveError = true
                            }
                        } else {
                            if (isOverlapping(
                                            getDate(startHours, startMinutes),
                                            getDate(endHours, endMinutes),
                                            getDate(startHours2, startMinutes2),
                                            getDate(endHours2, endMinutes2)
                                    )) {
                                longToast(getString(R.string.time_of_classes_must_not_intersect))
                                haveError = true
                            }
                        }
                    }

                    if (!haveError) {
                        //переназначаем порядковые номера парам
                        tempClassesList = tempClassesList.mapIndexed { index, arrayOfPairs ->
                            arrayOfPairs.mapIndexed { pairSecondIndex, pair ->
                                if (pairSecondIndex == 7) {
                                    pair.first to "${getString(R.string.class_serial_number)}${index + 1}"
                                } else pair
                            }.toTypedArray()
                        }.toMutableList()

                        database.use {
                            delete(SCHEDULE_TABLE_NAME, "($KEY_DAY_OF_WEEK = {$KEY_DAY_OF_WEEK})",
                                    KEY_DAY_OF_WEEK to selectedDay)

                            for (_class in tempClassesList)
                                insert(SCHEDULE_TABLE_NAME, *_class)
                        }

                        setResult(RESULT_OK)
                        finish()
                    }
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    private fun getDate(hours: Int, minutes: Int): Date {
        val gregorianCalendar = GregorianCalendar()
        gregorianCalendar.set(Calendar.HOUR_OF_DAY, hours)
        gregorianCalendar.set(Calendar.MINUTE, minutes)
        return gregorianCalendar.time
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getDateJava8(hours: Int, minutes: Int): LocalTime {
        return LocalTime.of(hours, minutes)
    }

    private fun isOverlapping(start1: Date, end1: Date, start2: Date, end2: Date): Boolean {
        return start1.before(end2) && start2.before(end1)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun isOverlappingJava8(startA: LocalTime, endA: LocalTime, startB: LocalTime, endB: LocalTime): Boolean {
        return startA.isBefore(endB) && endA.isAfter(startB)
    }
}