package fr.wicowyn.pokehelper.adapter;

import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by yapiti on 17/09/16.
 */
public abstract class RecyclerArrayAdapter<T extends RecyclerView.ViewHolder, V> extends RecyclerView.Adapter<T> {
    private ArrayList<V> list=new ArrayList<>();


    @Override
    public int getItemCount() {
        return list.size();
    }

    public V getItem(int index) {
        return list.get(index);
    }

    public boolean add(V object) {
        int position=getItemCount();
        boolean result=list.add(object);

        if(result){
            notifyItemInserted(position);
        }

        return result;
    }

    public void set(int position, V object) {
        list.set(position, object);

        notifyItemChanged(position);
    }

    public void insert(int position, V object) {
        list.add(position, object);

        notifyItemInserted(position);
    }

    public boolean addAll(Collection<? extends V> collection) {
        int start=list.size();
        boolean result=list.addAll(collection);

        if(result){
            notifyItemRangeInserted(start, getItemCount());
        }

        return result;
    }

    public void setAll(Collection<? extends V> collection) {
        boolean empty=list.isEmpty();

        list.clear();
        list.addAll(collection);

        if(empty) {
            notifyItemRangeInserted(0, list.size());
        }
        else {
            notifyDataSetChanged();
        }
    }

    public boolean remove(V object) {
        int position=list.indexOf(object);
        boolean result=list.remove(object);

        if(result){
            notifyItemRemoved(position);
        }

        return result;
    }

    public boolean remove(int position) {
        V result=list.remove(position);

        if(result != null){
            notifyItemRemoved(position);
        }

        return result != null;
    }

    public void move(int from, int to) {
        V object = getItem(from);

        list.remove(object);
        list.add(to, object);

        notifyItemMoved(from, to);
    }

    public int position(V object) {
        return list.indexOf(object);
    }

    public void clear() {
        int count=list.size();
        list.clear();

        notifyItemRangeRemoved(0, count);
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public ArrayList<V> getList() {
        return new ArrayList<>(list);
    }

    public void sort(Comparator<V> comparator) {
        Collections.sort(list, comparator);

        notifyItemRangeChanged(0, list.size());
    }
}
