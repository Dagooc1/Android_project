package com.example.parkingfinder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.view.ViewGroup.LayoutParams;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder> {

    private Context context;
    private OnboardingListener onboardingListener;

    // Data for onboarding screens
    private int[] illustrations = {
            R.drawable.location,
            R.drawable.motorcycle,
            R.drawable.sign
    };

    private String[] titles = {
            "Welcome to",
            "Save your",
            "Navigate Back"
    };

    private String[] subtitles = {
            "Save Parking Buksu",
            "Parking location instantly",
            "with ease"
    };

    private String[] descriptions = {
            "Never forget where you parked your vehicle inside Bukidnon State University again. Let's make your campus journey easier.",
            "With one tap, save your exact parking spot using GPS and sensor technologies",
            "Open the app anytime to get guided directions back to your vehicle"
    };

    public OnboardingAdapter(Context context, OnboardingListener listener) {
        this.context = context;
        this.onboardingListener = listener;
    }

    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_onboarding_screen, parent, false);
        return new OnboardingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        holder.illustrationView.setImageResource(illustrations[position]);
        holder.titleView.setText(titles[position]);
        holder.subtitleView.setText(subtitles[position]);
        holder.descriptionView.setText(descriptions[position]);

        // Set button visibility and style based on position
        if (position == getItemCount() - 1) {
            // Last screen - show "Get Started" button
            holder.nextButton.setText(">");

            // Make the button bigger for the last screen
            ViewGroup.LayoutParams params = holder.nextButton.getLayoutParams();
            params.width = context.getResources().getDimensionPixelSize(R.dimen.get_started_button_width);
            holder.nextButton.setLayoutParams(params);

            // Add some padding for text
            holder.nextButton.setPadding(
                    context.getResources().getDimensionPixelSize(R.dimen.get_started_padding_horizontal),
                    holder.nextButton.getPaddingTop(),
                    context.getResources().getDimensionPixelSize(R.dimen.get_started_padding_horizontal),
                    holder.nextButton.getPaddingBottom()
            );
        } else {
            // Regular "next" button for other screens
            holder.nextButton.setText(context.getString(R.string.next_arrow));

            // Reset to original size
            ViewGroup.LayoutParams params = holder.nextButton.getLayoutParams();
            params.width = context.getResources().getDimensionPixelSize(R.dimen.nav_button_size);
            holder.nextButton.setLayoutParams(params);

            // Reset padding
            holder.nextButton.setPadding(0, 0, 0, 0);
        }

        // Set indicator colors based on position
        holder.updateIndicators(position);

        // Button click listeners
        holder.nextButton.setOnClickListener(v -> {
            if (position == getItemCount() - 1) {
                onboardingListener.onFinishOnboarding();
            } else {
                onboardingListener.onNextPage(position + 1);
            }
        });

        holder.prevButton.setOnClickListener(v -> {
            if (position > 0) {
                onboardingListener.onNextPage(position - 1);
            }
        });

        // Show/hide prev button based on position
        if (position == 0) {
            holder.prevButton.setVisibility(View.INVISIBLE);
        } else {
            holder.prevButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return titles.length;
    }

    public class OnboardingViewHolder extends RecyclerView.ViewHolder {
        ImageView illustrationView;
        TextView titleView, subtitleView, descriptionView;
        Button nextButton, prevButton;
        ImageView indicator1, indicator2, indicator3;

        public OnboardingViewHolder(@NonNull View itemView) {
            super(itemView);
            illustrationView = itemView.findViewById(R.id.onboardingIllustration);
            titleView = itemView.findViewById(R.id.onboardingTitle);
            subtitleView = itemView.findViewById(R.id.onboardingSubtitle);
            descriptionView = itemView.findViewById(R.id.onboardingDescription);
            nextButton = itemView.findViewById(R.id.nextButton);
            prevButton = itemView.findViewById(R.id.prevButton);
            indicator1 = itemView.findViewById(R.id.indicator1);
            indicator2 = itemView.findViewById(R.id.indicator2);
            indicator3 = itemView.findViewById(R.id.indicator3);
        }

        public void updateIndicators(int position) {
            // Reset all indicators to inactive
            indicator1.setImageResource(R.drawable.indicator_inactive);
            indicator2.setImageResource(R.drawable.indicator_inactive);
            indicator3.setImageResource(R.drawable.indicator_inactive);

            // Set active indicator based on position
            switch (position) {
                case 0:
                    indicator1.setImageResource(R.drawable.indicator_active);
                    break;
                case 1:
                    indicator2.setImageResource(R.drawable.indicator_active);
                    break;
                case 2:
                    indicator3.setImageResource(R.drawable.indicator_active);
                    break;
            }
        }
    }

    public interface OnboardingListener {
        void onNextPage(int position);
        void onFinishOnboarding();
    }
}