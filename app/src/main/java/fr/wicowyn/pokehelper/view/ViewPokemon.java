package fr.wicowyn.pokehelper.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.pokegoapi.api.pokemon.Pokemon;

import butterknife.Bind;
import butterknife.ButterKnife;
import fr.wicowyn.pokehelper.R;

/**
 * Created by Eliot on 17/08/2016.
 */
public class ViewPokemon extends FrameLayout {

    @Bind(R.id.pokemon_name) TextView pokemonName;

    public ViewPokemon(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.pokemon_view, this);
        ButterKnife.bind(this);
    }

    public void setPokemon(Pokemon pokemon) {
        pokemonName.setText("" + pokemon.getStamina());
    }
}