package jp.cloud.marking.android.cloudalbum;

import android.content.Context;
import android.graphics.Point;
import android.support.v7.widget.RecyclerView;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Map;

import static android.content.Context.WINDOW_SERVICE;


public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
    private Context mContext;
    private LayoutInflater mInflater;
    private List<Map<String, Object>> mImageList;
    private int mImageSize;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageview;

        public ViewHolder(View v) {
            super(v);
            imageview = (ImageView) v.findViewById(R.id.image);
        }
    }

    public ImageAdapter(Context context, List<Map<String, Object>> imageList) {
        mContext = context;
        mImageList = imageList;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        WindowManager wm = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        Display disp = wm.getDefaultDisplay();

        Point realSize = new Point();
        disp.getRealSize(realSize);

        mImageSize = realSize.x / 3;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = mInflater.inflate(R.layout.cell_image_list, parent, false);
        return new ImageAdapter.ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Map<String, Object> image = mImageList.get(position);
        Picasso.with(mContext)
                .load((String) image.get(MainActivity.FIRESTORE_KEY_FILE_URL))
                .placeholder(R.drawable.progress_animation)
                .resize(mImageSize, mImageSize)
                .centerCrop()
                .into(holder.imageview);
    }

    @Override
    public int getItemCount() {
        return mImageList.size();
    }
}
