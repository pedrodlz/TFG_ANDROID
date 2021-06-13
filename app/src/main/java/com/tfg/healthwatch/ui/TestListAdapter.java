package com.tfg.healthwatch.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.tfg.healthwatch.R;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;

public class TestListAdapter extends RecyclerView.Adapter<TestListAdapter.Question>  {

    private Context context;
    private ArrayList<TestsFragment.Question> questionsList;
    private FirebaseUser currentUser;
    private DatabaseReference responsesTable;
    private String type;

    public TestListAdapter(Context ct, ArrayList<TestsFragment.Question> questionsList, String type){
        context = ct;
        this.questionsList = questionsList;
        this.type = type;
    }

    @NonNull
    @NotNull
    @Override
    public Question onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.question_list_item,parent, false);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        LocalDate date= LocalDate.now( ZoneOffset.UTC ) ;
        String stringDate= "" + date.getDayOfMonth() + date.getMonthValue() + date.getYear();
        responsesTable = FirebaseDatabase.getInstance().getReference().child("Responses").child(currentUser.getUid()).child(stringDate).child(type);

        return new TestListAdapter.Question(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull Question holder, int position) {
        String showText = questionsList.get(position).text;
        holder.questionText.setText(showText);

        holder.star1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.star1.setImageResource(R.drawable.ic_star_filled_svgrepo_com);
                holder.star2.setImageResource(R.drawable.ic_star_empty_svgrepo_com);
                holder.star3.setImageResource(R.drawable.ic_star_empty_svgrepo_com);
                holder.star4.setImageResource(R.drawable.ic_star_empty_svgrepo_com);
                holder.star5.setImageResource(R.drawable.ic_star_empty_svgrepo_com);
                responsesTable.child(questionsList.get(position).id).setValue(1);
            }
        });

        holder.star2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.star1.setImageResource(R.drawable.ic_star_filled_svgrepo_com);
                holder.star2.setImageResource(R.drawable.ic_star_filled_svgrepo_com);
                holder.star3.setImageResource(R.drawable.ic_star_empty_svgrepo_com);
                holder.star4.setImageResource(R.drawable.ic_star_empty_svgrepo_com);
                holder.star5.setImageResource(R.drawable.ic_star_empty_svgrepo_com);

                responsesTable.child(questionsList.get(position).id).setValue(2);
            }
        });

        holder.star3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.star1.setImageResource(R.drawable.ic_star_filled_svgrepo_com);
                holder.star2.setImageResource(R.drawable.ic_star_filled_svgrepo_com);
                holder.star3.setImageResource(R.drawable.ic_star_filled_svgrepo_com);
                holder.star4.setImageResource(R.drawable.ic_star_empty_svgrepo_com);
                holder.star5.setImageResource(R.drawable.ic_star_empty_svgrepo_com);
                responsesTable.child(questionsList.get(position).id).setValue(3);
            }
        });

        holder.star4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.star1.setImageResource(R.drawable.ic_star_filled_svgrepo_com);
                holder.star2.setImageResource(R.drawable.ic_star_filled_svgrepo_com);
                holder.star3.setImageResource(R.drawable.ic_star_filled_svgrepo_com);
                holder.star4.setImageResource(R.drawable.ic_star_filled_svgrepo_com);
                holder.star5.setImageResource(R.drawable.ic_star_empty_svgrepo_com);
                responsesTable.child(questionsList.get(position).id).setValue(4);
            }
        });

        holder.star5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.star1.setImageResource(R.drawable.ic_star_filled_svgrepo_com);
                holder.star2.setImageResource(R.drawable.ic_star_filled_svgrepo_com);
                holder.star3.setImageResource(R.drawable.ic_star_filled_svgrepo_com);
                holder.star4.setImageResource(R.drawable.ic_star_filled_svgrepo_com);
                holder.star5.setImageResource(R.drawable.ic_star_filled_svgrepo_com);
                responsesTable.child(questionsList.get(position).id).setValue(5);
            }
        });

    }

    @Override
    public int getItemCount() {
        return questionsList.size();
    }

    public class Question extends RecyclerView.ViewHolder{

        TextView questionText;
        ImageButton star1,star2,star3,star4,star5;

        public Question(@NonNull View itemView) {
            super(itemView);

            questionText = itemView.findViewById(R.id.question_text);
            star1 = itemView.findViewById(R.id.starButton1);
            star2 = itemView.findViewById(R.id.starButton2);
            star3 = itemView.findViewById(R.id.starButton3);
            star4 = itemView.findViewById(R.id.starButton4);
            star5 = itemView.findViewById(R.id.starButton5);
        }
    }
}
