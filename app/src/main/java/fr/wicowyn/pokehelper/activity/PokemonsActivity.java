package fr.wicowyn.pokehelper.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import butterknife.Bind;
import butterknife.ButterKnife;
import fr.wicowyn.pokehelper.R;
import fr.wicowyn.pokehelper.adapter.PokemonsAdapter;
import fr.wicowyn.pokehelper.api.PokAPI;

public class PokemonsActivity extends BaseActivity {

    @Bind(R.id.pokemons_recycler_view) RecyclerView pokemonRecyclerView;
    private PokemonsAdapter pokemonsAdapter = new PokemonsAdapter();;

    public static Intent newIntent(Context context){
        return new Intent(context, PokemonsActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pokemons);
        ButterKnife.bind(this);

        pokemonRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        PokAPI.pokemons().subscribe(pokemonsAdapter::setAll);
        pokemonRecyclerView.setAdapter(pokemonsAdapter);
    }


}
