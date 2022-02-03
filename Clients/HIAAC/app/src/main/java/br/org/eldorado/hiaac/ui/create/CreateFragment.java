package br.org.eldorado.hiaac.ui.create;

import static br.org.eldorado.hiaac.FileSelectorActivity.PLOT_TYPE;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import br.org.eldorado.hiaac.FileSelectorActivity;
import br.org.eldorado.hiaac.R;
import br.org.eldorado.hiaac.databinding.FragmentCreateBinding;

public class CreateFragment extends Fragment {

    private CreateViewModel createViewModel;
    private FragmentCreateBinding binding;
    private CardView umapCardView;
    private CardView tsneCardView;
    private CardView isomapCardView;
    private CardView lleCardView;
    private CardView dmlCardView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        createViewModel =
                new ViewModelProvider(this).get(CreateViewModel.class);

        binding = FragmentCreateBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        umapCardView = root.findViewById(R.id.umap_card_view);
        umapCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(root.getContext(), FileSelectorActivity.class);
                intent.putExtra(PLOT_TYPE, "plotUmap");
                startActivity(intent);
            }
        });

        tsneCardView = root.findViewById(R.id.tsne_card_view);
        tsneCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(root.getContext(), FileSelectorActivity.class);
                intent.putExtra(PLOT_TYPE, "plotTsne");
                startActivity(intent);
            }
        });

        isomapCardView = root.findViewById(R.id.isomap_card_view);
        isomapCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(root.getContext(), FileSelectorActivity.class);
                intent.putExtra(PLOT_TYPE, "plotIsomap");
                startActivity(intent);
            }
        });

        lleCardView = root.findViewById(R.id.lle_card_view);
        lleCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(root.getContext(), FileSelectorActivity.class);
                intent.putExtra(PLOT_TYPE, "plotLle");
                startActivity(intent);
            }
        });

        dmlCardView = root.findViewById(R.id.dml_card_view);
        dmlCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(root.getContext(), FileSelectorActivity.class);
                intent.putExtra(PLOT_TYPE, "plotDml");
                startActivity(intent);
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}