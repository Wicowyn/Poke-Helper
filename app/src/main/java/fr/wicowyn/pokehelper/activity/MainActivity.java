package fr.wicowyn.pokehelper.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.pokegoapi.util.Log;

import POGOProtos.Data.PlayerDataOuterClass;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.wicowyn.pokehelper.R;
import fr.wicowyn.pokehelper.api.PokAPI;

public class MainActivity extends BaseActivity {
    @Bind(R.id.name)
    TextView name;


    public static Intent newIntent(Context context) {
        return new Intent(context, MainActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        PokAPI.profileData().map(PlayerDataOuterClass.PlayerData::getUsername).subscribe(s -> {
            name.setText(s);
            Log.i("user_name", s);
        });

    }

    @OnClick(R.id.map_pokestop)
    public void onPokestopMap() {
        startActivity(PokestopActivity.newIntent(this));
    }
}
