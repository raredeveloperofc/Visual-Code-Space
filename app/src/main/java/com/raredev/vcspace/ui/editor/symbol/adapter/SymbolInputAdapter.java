package com.raredev.vcspace.ui.editor.symbol.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.raredev.vcspace.databinding.LayoutSymbolItemBinding;
import com.raredev.vcspace.ui.editor.Symbol;
import io.github.rosemoe.sora.widget.CodeEditor;
import java.util.List;

public class SymbolInputAdapter extends RecyclerView.Adapter<SymbolInputAdapter.VH> {

  private List<Symbol> symbolList;

  private CodeEditor editor;

  @NonNull
  @Override
  public VH onCreateViewHolder(ViewGroup parent, int arg1) {
    return new VH(
        LayoutSymbolItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
  }

  @Override
  public void onBindViewHolder(VH holder, int position) {
    Symbol symbol = symbolList.get(position);
    holder.itemView.setAnimation(
        AnimationUtils.loadAnimation(holder.itemView.getContext(), android.R.anim.fade_in));

    holder.label.setText(symbol.getLabel());
    holder.itemView.setOnClickListener(
        v -> {
          if (editor != null && editor.isEditable()) {
            if ("\t".equals(symbol.getInsert()) && editor.getSnippetController().isInSnippet()) {
              editor.getSnippetController().shiftToNextTabStop();
            } else {
              editor.commitText(symbol.getInsert());
            }
          }
        });
  }

  @Override
  public int getItemCount() {
    return symbolList.size();
  }

  public void setSymbols(List<Symbol> symbols) {
    this.symbolList = symbols;
    notifyDataSetChanged();
  }

  public void bindEditor(@NonNull CodeEditor editor) {
    this.editor = editor;
  }

  public class VH extends RecyclerView.ViewHolder {
    TextView label;

    public VH(LayoutSymbolItemBinding binding) {
      super(binding.getRoot());
      label = binding.label;
    }
  }
}
