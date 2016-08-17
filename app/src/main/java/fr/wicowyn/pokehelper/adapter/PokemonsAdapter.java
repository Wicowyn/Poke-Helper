package fr.wicowyn.pokehelper.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.pokegoapi.api.pokemon.Pokemon;

import fr.wicowyn.pokehelper.view.ViewPokemon;

/**
 * Created by Eliot on 17/08/2016.
 */
public class PokemonsAdapter extends RecyclerArrayAdapter<PokemonsAdapter.Holder, Pokemon> {


    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = new ViewPokemon(parent.getContext());
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        ((ViewPokemon) holder.itemView).setPokemon(getItem(position));
    }

    public static class Holder extends RecyclerView.ViewHolder {

        public Holder(View itemView) {
            super(itemView);
        }
    }
}
