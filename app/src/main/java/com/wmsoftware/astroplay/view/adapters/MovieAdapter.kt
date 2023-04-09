package com.wmsoftware.astroplay.view.adapters

import android.content.Intent
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.wmsoftware.astroplay.databinding.MovieCardBinding
import com.wmsoftware.astroplay.model.Movie
import com.wmsoftware.astroplay.view.MovieDetailActivity
import java.io.Serializable

class MovieAdapter(private val movies: List<Movie>) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    inner class MovieViewHolder(val binding: MovieCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = MovieCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MovieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = movies[position]

        holder.binding.movieTitle.text = movie.title
        holder.binding.movieGenre.text = movie.genre?.get(0)

        Glide.with(holder.binding.moviePoster.context)
            .load(movie.poster)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.binding.moviePoster)


        holder.binding.movieLayout.setOnClickListener {
                val context = holder.binding.moviePoster.context
                val intent = Intent(context, MovieDetailActivity::class.java)
                intent.putExtra("movie", movies[position] as Parcelable)
                context?.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = movies.size
}