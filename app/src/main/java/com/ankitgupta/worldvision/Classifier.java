package com.ankitgupta.worldvision;

import java.util.List;
import android.graphics.Bitmap;

public interface Classifier {

    class Recognition {

        private final Float confid;
        private final String name;
        private final String label;

        public Recognition(final String i, final String n, final Float c) {
            this.label = i;
            this.name = n;
            this.confid = c;
        }

        public String getLabel() {
            return label;
        } //Helper Methods
        public String getName() {
            return name;
        }
        public Float getConfid() {
            return confid;
        }

        @Override
        public String toString() {
            String toretstr = "";
            if (label != null) {
                toretstr += "[" + label + "] ";
            }
            if (name != null) {
                toretstr += name + " ";
            }
            if (confid != null) {
                toretstr += String.format("(%.1f%%) ", confid * 100.0f);
            }
            String finalret = toretstr.trim();
            return finalret;
        }
    }

    List<Recognition> recognizeImage(Bitmap bitmap); //Recognize the Image
    void close(); //Close out
}
