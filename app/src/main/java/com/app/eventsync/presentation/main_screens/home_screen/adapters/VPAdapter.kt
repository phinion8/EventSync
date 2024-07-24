package com.app.eventsync.presentation.main_screens.home_screen.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import java.util.ArrayList

class VPAdapter(
    fm: FragmentManager
) : FragmentStatePagerAdapter(fm) {

    private val fragmentList: java.util.ArrayList<Fragment> = ArrayList()
    private val fragmentTitle: java.util.ArrayList<String> = ArrayList()

    override fun getCount(): Int {
        return fragmentList.size
    }

    override fun getItem(position: Int): Fragment {
        return fragmentList[position]
    }

    fun addFragment(fragment: Fragment, title: String){
        fragmentList.add(fragment)
        fragmentTitle.add(title)
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return fragmentTitle[position]
    }

}