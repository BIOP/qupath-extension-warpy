package net.imglib2.realtransform;

import net.imglib2.RealInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;

/**
 * RealTransform object limiting the extend of the transformation computation.
 *
 * Used in BigDataViewer to speed up display
 */
public class BoundedRealTransform implements InvertibleRealTransform {

    final InvertibleRealTransform origin;
    final RealInterval interval;
    final int nDimSource, nDimTarget;

    public BoundedRealTransform(InvertibleRealTransform origin, RealInterval interval) {
        this.origin = origin;
        this.interval = interval;
        nDimSource = origin.numSourceDimensions();
        nDimTarget = origin.numTargetDimensions();
    }

    @Override
    public int numSourceDimensions() {
        return nDimSource;
    }

    @Override
    public int numTargetDimensions() {
        return nDimTarget;
    }

    @Override
    public void apply(double[] source, double[] target) {
        origin.apply(source,target);
    }

    @Override
    public void apply(RealLocalizable realLocalizable, RealPositionable realPositionable) {
        boolean inBounds = true;
        for (int d = 0; d < nDimSource; d++) {
            if (realLocalizable.getFloatPosition(d)<interval.realMin(d)) {
                inBounds = false;
                break;
            }
            if (realLocalizable.getFloatPosition(d)>interval.realMax(d)) {
                inBounds = false;
                break;
            }
        }
        if (inBounds) {
            origin.apply(realLocalizable, realPositionable);
        } else {
            realPositionable.setPosition(realLocalizable);
        }
    }

    @Override
    public void applyInverse(double[] source, double[] target) {
        origin.applyInverse(source, target);
    }

    @Override
    public void applyInverse(RealPositionable realPositionable, RealLocalizable realLocalizable) {
        origin.applyInverse(realPositionable, realLocalizable);
    }

    @Override
    public InvertibleRealTransform inverse() {
        return origin.inverse();
    }

    @Override
    public InvertibleRealTransform copy() {
        return new BoundedRealTransform(origin.copy(), interval);
    }

    public RealInterval getInterval() {
        return interval;
    }

    public RealTransform getTransform() {
        return origin;
    }

}