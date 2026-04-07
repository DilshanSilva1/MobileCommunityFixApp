package com.example.communityapp.ui.posts

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.communityapp.R
import com.example.communityapp.databinding.ActivityMainBinding
import com.example.communityapp.ui.auth.AuthActivity
import com.example.communityapp.ui.auth.AuthState
import com.example.communityapp.ui.auth.AuthViewModel
import com.example.communityapp.utils.StatusBarUtils
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var drawerToggle: ActionBarDrawerToggle

    val authViewModel: AuthViewModel by viewModels()
    val postsViewModel: PostsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Force beige status bar — must be called after setContentView
        StatusBarUtils.applyBeige(this)

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.postsListFragment, R.id.myPostsFragment, R.id.myFavoritesFragment, R.id.profileFragment),
            binding.drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        drawerToggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.drawer_open, R.string.drawer_close
        )
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)

        authViewModel.currentUser.observe(this) { user ->
            postsViewModel.currentUser = user
            user?.let { updateDrawerHeader(it.displayName, it.email, it.moderator) }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.postsListFragment -> binding.navView.setCheckedItem(R.id.nav_all_posts)
                R.id.myPostsFragment -> binding.navView.setCheckedItem(R.id.nav_my_posts)
                R.id.myFavoritesFragment -> binding.navView.setCheckedItem(R.id.nav_my_favorites)
                R.id.profileFragment -> binding.navView.setCheckedItem(R.id.nav_profile)
            }
        }

        authViewModel.authState.observe(this) { state ->
            if (state is AuthState.Unauthenticated && isTaskRoot) {
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
            }
        }
    }

    private fun updateDrawerHeader(name: String, email: String, isModerator: Boolean) {
        val header = binding.navView.getHeaderView(0)
        header.findViewById<TextView>(R.id.tv_drawer_name).text = name
        header.findViewById<TextView>(R.id.tv_drawer_email).text = email
        header.findViewById<TextView>(R.id.tv_drawer_role).text =
            if (isModerator) "Moderator" else "Member"
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_all_posts -> {
                if (navController.currentDestination?.id != R.id.postsListFragment)
                    navController.navigate(R.id.postsListFragment)
            }
            R.id.nav_my_posts -> {
                if (navController.currentDestination?.id != R.id.myPostsFragment)
                    navController.navigate(R.id.myPostsFragment)
            }
            R.id.nav_my_favorites -> {
                if (navController.currentDestination?.id != R.id.myFavoritesFragment)
                    navController.navigate(R.id.myFavoritesFragment)
            }
            R.id.nav_profile -> {
                if (navController.currentDestination?.id != R.id.profileFragment)
                    navController.navigate(R.id.profileFragment)
            }
            R.id.nav_logout -> authViewModel.logout()
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}