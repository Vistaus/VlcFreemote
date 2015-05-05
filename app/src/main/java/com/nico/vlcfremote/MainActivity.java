package com.nico.vlcfremote;

import android.content.SharedPreferences;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nico.vlcfremote.utils.VlcConnector;

import java.util.List;

public class MainActivity extends ActionBarActivity
                            implements SeekBar.OnSeekBarChangeListener,
                                       VlcConnector.VlcConnectionCallback,
                                       VlcConnector.VlcConnectionHandler,
                                       ServerSelectView.OnServerSelectedCallback {

    private VlcConnector vlc;
    private DirListingFragment dirlistView;
    private PlaylistFragment playlistView;
    private ServerSelectView serverSelectView;
    private MainMenuNavigation mainMenu;

    private class MainMenuNavigation extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener {

        private final PlaylistFragment playlistView;
        private final DirListingFragment dirlistView;
        private final ServerSelectView serverSelectView;
        private final ViewPager parentView;

        public MainMenuNavigation(ViewPager view, FragmentManager fm, PlaylistFragment playlistView,
                                   DirListingFragment dirlistView, ServerSelectView serverSelectView)
        {
            super(fm);
            this.parentView = view;
            this.playlistView = playlistView;
            this.dirlistView = dirlistView;
            this.serverSelectView = serverSelectView;
            
            parentView.setAdapter(this);
            parentView.setOnPageChangeListener(this);
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0: return playlistView;
                case 1: return dirlistView;
                case 2: return serverSelectView;
                default: throw new RuntimeException(MainMenuNavigation.class.getName() + " tried to select a page item which doesn't exist.");
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return getString(R.string.main_menu_title_playlist);
                case 1: return getString(R.string.main_menu_title_dir_listing);
                case 2: return getString(R.string.main_menu_title_servers);
                default: throw new RuntimeException(MainMenuNavigation.class.getName() + " tried to get a title for a page which doesn't exist.");
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override public void onPageScrolled(int i, float v, int i2) {}
        @Override public void onPageScrollStateChanged(int i) {}

        @Override
        public void onPageSelected(int i) {
            switch (i) {
                case 0: playlistView.updatePlaylist(); return;
                case 1: dirlistView.updateDirectoryList(); return;
                case 2: serverSelectView.scanServers(); return;
                default: throw new RuntimeException(MainMenuNavigation.class.getName() + " selected a page which doesn't exist.");
            }
        }

        public void jumpToServersPage() {
            parentView.setCurrentItem(2, true);
        }

        public void jumpToPlaylistPage() {
            parentView.setCurrentItem(0, true);
        }
    }

    @Override
    public void onNewServerSelected(String ip, String port, String password) {
        SharedPreferences.Editor cfg = getPreferences(0).edit();
        cfg.putString("VLC_Last_Server_IP", ip);
        cfg.putString("VLC_Last_Server_Port", port);
        cfg.putString("VLC_Last_Server_Pass", password);
        cfg.apply();

        this.vlc = new VlcConnector(ip, port, password);
        this.playlistView.updatePlaylist();
        this.dirlistView.updateDirectoryList();
        this.mainMenu.jumpToPlaylistPage();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // Will connect to a dummy server first time and jump directly to the servers page
        final String lastServer_IP = getPreferences(0).getString("VLC_Last_Server_IP", "localhost");
        final String lastServer_Port = getPreferences(0).getString("VLC_Last_Server_Port", "8080");
        final String lastServer_Pass = getPreferences(0).getString("VLC_Last_Server_Pass", "dummy_pass");
        this.vlc = new VlcConnector(lastServer_IP, lastServer_Port, lastServer_Pass);
        Log.i("ASD", String.valueOf(this.vlc));

        this.playlistView = new PlaylistFragment();
        this.dirlistView = new DirListingFragment();
        this.serverSelectView = new ServerSelectView();

        this.mainMenu = new MainMenuNavigation(((ViewPager) super.findViewById(R.id.wMainMenu)),
                                                getSupportFragmentManager(), playlistView, dirlistView, serverSelectView);

        ((SeekBar) findViewById(R.id.wPlayer_Volume)).setOnSeekBarChangeListener(this);

        // TODO
        vlc.getStatus(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //noinspection SimplifiableIfStatement
        if (item.getItemId() == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public VlcConnector getVlcConnector() {
        return vlc;
    }

    public void onPlayPauseClick(View view) {
        getVlcConnector().togglePlay();
    }

    public void onPlayPreviousClick(View view) {
        getVlcConnector().playPrevious();
    }

    public void onPlayNextClick(View view) {
        getVlcConnector().playNext();
    }

    public void onPlayPosition_JumpBack(View view) {
        getVlcConnector().playPosition_JumpRelative(-0.5);
    }

    public void onPlayPosition_JumpForward(View view) {
        getVlcConnector().playPosition_JumpRelative(0.5);
    }

    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        getVlcConnector().setVolume(progress);
    }

    @Override
    public void Vlc_OnStatusUpdated(VlcConnector.VlcStatus stat) {
        ((TextView) findViewById(R.id.wPlayer_CurrentlyPlaying)).setText("Current status: " + stat.state);
    }

    // Handle VLC callbacks
    @Override public void Vlc_OnAddedToPlaylistCallback(Integer addedMediaId) { this.Vlc_OnProgrammingError(); }
    @Override public void Vlc_OnPlaylistFetched(List<VlcConnector.PlaylistEntry> contents) { this.Vlc_OnProgrammingError(); }
    @Override public void Vlc_OnDirListingFetched(String requestedPath, List<VlcConnector.DirListEntry> contents) { this.Vlc_OnProgrammingError(); }


    @Override
    public void Vlc_OnLoginIncorrect() {
        CharSequence msg = getResources().getString(R.string.status_vlc_wrong_password);
        Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
        toast.show();

        mainMenu.jumpToServersPage();
    }

    @Override
    public void Vlc_OnConnectionFail() {
        CharSequence msg = getResources().getString(R.string.status_vlc_cant_connect);
        Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
        toast.show();

        mainMenu.jumpToServersPage();
    }

    @Override
    public void Vlc_OnInternalError(Throwable ex) {
        CharSequence msg = getResources().getString(R.string.stats_vlc_parser_internal_error);
        Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
        toast.show();
    }

    @Override
    public void Vlc_OnInvalidResponseReceived(Throwable ex) {
        CharSequence msg = getResources().getString(R.string.status_invalid_vlc_response);
        Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
        toast.show();
    }

    @Override
    public void Vlc_OnProgrammingError() {
        CharSequence msg = getResources().getString(R.string.status_assertion_failed);
        Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
        toast.show();
    }
}
