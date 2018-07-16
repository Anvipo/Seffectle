package com.dNDTeam.seffectle.main

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.PagerAdapter
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import com.dNDTeam.seffectle.R
import com.dNDTeam.seffectle.R.string.today_is
import com.dNDTeam.seffectle.db.ScheduleMSQLOH
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.SCHEDULE_TABLE_NAME
import com.dNDTeam.seffectle.db.database
import com.dNDTeam.seffectle.db.getVKUserInfoFrom
import com.dNDTeam.seffectle.main.MainActivity.MyDayOfWeek.*
import com.dNDTeam.seffectle.restClient.SynchronizeActivity
import com.dNDTeam.seffectle.scheduleActions.ScheduleActionsActivity
import com.dNDTeam.seffectle.vk.LoginActivity
import com.dNDTeam.seffectle.vk.SendFeedbackActivity
import com.dNDTeam.seffectle.vk.VKUser
import com.vk.sdk.VKSdk
import kotlinx.android.synthetic.main.activity_main.container_AM
import kotlinx.android.synthetic.main.activity_main.tabs_AM
import kotlinx.android.synthetic.main.activity_main.toolbar_AM
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.db.dropTable
import org.jetbrains.anko.longToast
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.toast
import java.time.LocalDate
import java.util.Calendar
import java.util.GregorianCalendar

//использовать android architecture components
//https://developer.android.com/topic/libraries/architecture/guide.html
//https://github.com/googlesamples/android-architecture-components
//https://www.androidhive.info/2015/04/android-getting-started-with-material-design/
//поизучать
//https://blog.elpassion.com/create-a-clean-code-app-with-kotlin-coroutines-and-android-architecture-components-part-3-f3f3850acbe6

//https://material.io/guidelines/style/color.html#color-color-palette

/*
//https://github.com/Kotlin/anko/wiki - поизучать anko
//https://github.com/Kotlin/anko

//https://developer.android.com/about/dashboards/index.html
//https://developer.android.com/about/versions/android-5.0-changes.html - прочитать про изменения в android 6.0
//https://developer.android.com/about/versions/marshmallow/android-6.0-changes.html - прочитать про изменения в android 6.0
//https://developer.android.com/about/versions/nougat/android-7.0-changes.html - прочитать про изменения в android 7.0
//https://developer.android.com/about/versions/oreo/android-8.0-changes.html - прочитать про изменения в android 8.0
*/

class MainActivity : AppCompatActivity() {
    /*//TODO список фич для реализации (сверху те, которые вроде легче и быстрее реализовать)

    //TODO найти оптимальный inputType при вводе IP сервера

    //TODO если соединение с интернетом появилось, то попробовать получить/отправить данные
    //https://developer.android.com/training/monitoring-device-state/connectivity-monitoring.html
    //https://habrahabr.ru/post/149875/
    //https://developer.android.com/topic/performance/background-optimization.html
    //for Internet you have to ping any url and check response code.
    //TODO выбор расписания пар или каких-то своих дел (хорошая большая фича)

    //TODO сделать about с ссылками на разработчиков
    //TODO переделать send feedback куда-то в другое место (email мб)
    //TODO сделать где-нибудь ввод номера группы и подгруппы

    //TODO перенести элементы меню с работой с расписанием дня (изменить, очистить)
    //TODO в кнопки (значки, неважно), которые будут хранится в отдельной view
    //TODO которая будет добавляться автоматически в конец расписания дня (или мб как-то по-другому их добавлять)

    //TODO (возможно это хорошо) как только расписание на телефоне было изменено, передавать его сразу же на серв

    //TODO предлагать юзерам те расписания, которые есть у одногруппников (хорошая большая фича)
    //TODO добавить поддержку числителей и знаменателей в расписании
    //TODO привязать будильник (хорошая большая фича)

    //TODO выдавать возможное дз пользователю (хорошая большая фича)

    //TODO родительский контроль (хорошая большая фича)

    //TODO сделать к релизу
    //TODO заменить все toast(error) на Log.e(error)
    //TODO нормально закомменитить код (имена переменных вроде уже говорящие)
    //TODO закончить front*/

    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private var toastWasShown = false
    //https://habrahabr.ru/company/alfa/blog/336228/

    enum class MyDayOfWeek {
        MONDAY,
        TUESDAY,
        WEDNESDAY,
        THURSDAY,
        FRIDAY,
        SATURDAY,
        SUNDAY
    }

    //массив с названиями дней недели на языке пользователя
    private val daysOfWeek by lazy {
        resources.getStringArray(R.array.days_of_week)
    }

    //Автоматическая выборка нужной вкладки с расписанием и напоминание сегодняшнего дня
    private fun showGreeting(index: Int) {
        if (!toastWasShown) {
            //как обращаться к пользователю
            val address =
                if (VKSdk.isLoggedIn())
                    ", ${VKUser.vkAccountOwner.firstName} ${VKUser.vkAccountOwner.lastName}"
                else ""

            //когда index == Calendar.SUNDAY
            val temp = if (index == -1) SUNDAY.ordinal else index

            runOnUiThread {
                longToast("${getString(today_is)} - ${daysOfWeek[temp].toLowerCase()}$address")
                //когда temp == SUNDAY
                tabs_AM.getTabAt(if (temp == SUNDAY.ordinal) MONDAY.ordinal else index)?.select()
            }

            toastWasShown = true
        }
    }

    //функция для загрузки нужных данных
    @SuppressLint("NewApi")
    private fun loadDataAsync() = launch {
        getVKUserInfoFrom(database, this@MainActivity)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            showGreeting(LocalDate.now().dayOfWeek.value - 1)
        else
            showGreeting(GregorianCalendar().get(Calendar.DAY_OF_WEEK) - 2)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar_AM as Toolbar)

        setupViewPager()

        tabs_AM.setupWithViewPager(container_AM)

        loadDataAsync()
    }

    //заполнение ViewPager данными
    private fun setupViewPager() {
        container_AM.adapter = ViewPagerAdapter(supportFragmentManager).apply {
            for (dayOfWeek in values())
                addFragment(MainActivityFragment().apply {
                    arguments = bundleOf("selectedDay" to dayOfWeek.ordinal)
                }, daysOfWeek[dayOfWeek.ordinal])
        }
    }

    inner class ViewPagerAdapter(fragmentManager: FragmentManager) :
        FragmentPagerAdapter(fragmentManager) {
        private val fragments = mutableListOf<Fragment>()
        private val fragmentsTitles = mutableListOf<String>()

        fun addFragment(fragment: Fragment, title: String) {
            fragments.add(fragment)
            fragmentsTitles.add(title)
        }

        override fun getItem(position: Int) = fragments[position]

        override fun getCount() = fragments.size

        override fun getItemPosition(`object`: Any): Int = PagerAdapter.POSITION_NONE

        override fun getPageTitle(position: Int) = fragmentsTitles[position]
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(1, 1, 5, getString(R.string.log_out_from_vk))
        menu.add(2, 2, 5, getString(R.string.log_into_vk))
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.setGroupVisible(1, VKSdk.isLoggedIn())
        menu?.setGroupVisible(2, !VKSdk.isLoggedIn())
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        val selectedDay = tabs_AM.selectedTabPosition
        when (item.itemId) {
            R.id.action_change -> {
                startActivityForResult<ScheduleActionsActivity>(
                    1,
                    "selectedDay" to selectedDay
                )
                return true
            }
            R.id.action_synchronize -> {
                startActivityForResult<SynchronizeActivity>(2)

                return true
            }
            R.id.action_clear -> {
                database.use {
                    dropTable(SCHEDULE_TABLE_NAME)
//                    createTable()
                    ScheduleMSQLOH.getInstance(this@MainActivity).onCreate(this)
                    /*//котлиновская реализация delete защищает (судя по исходникам) от SQL-инъекций
                    delete(
                        SCHEDULE_TABLE_NAME, "($KEY_DAY_OF_WEEK = {$KEY_DAY_OF_WEEK})",
                        KEY_DAY_OF_WEEK to selectedDay
                    )*/
                }

                //Обновление вьюшки
                container_AM.adapter?.notifyDataSetChanged()
                return true
            }
            R.id.action_send_feedback -> {
                startActivityForResult<SendFeedbackActivity>(3)

                return true
            }
            1, 2 -> {
                startActivityForResult<LoginActivity>(5)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()

//        loadDataAsync()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                when (requestCode) {
                    1 -> toast(getString(R.string.schedule_was_edited_successfully))
                    2 -> toast(getString(R.string.schedule_was_downloaded_successfully))
                    3 -> toast(getString(R.string.feedback_was_uploaded_successfully))
                }

                container_AM.adapter?.notifyDataSetChanged()
            }
            Activity.RESULT_CANCELED -> when (requestCode) {
                1 -> toast(getString(R.string.schedule_was_not_edited))
                2 -> toast(getString(R.string.synchronization_was_not_complete))
                3 -> toast(getString(R.string.feedback_was_not_uploaded))
            }
            Activity.RESULT_FIRST_USER -> when (requestCode) {
                2 -> longToast(getString(R.string.you_must_log_in_to_use_the_server))
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putBoolean("toastWasShown", toastWasShown)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        toastWasShown = savedInstanceState!!.getBoolean("toastWasShown")
    }
}