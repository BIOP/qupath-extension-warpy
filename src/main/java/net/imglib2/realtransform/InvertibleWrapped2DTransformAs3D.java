package net.imglib2.realtransform;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;

/**
 * Class copied from bigwarp_fiji repo because to avoid bringing
 * all big warp dependencies.
 *
 * See original code https://github.com/saalfeldlab/bigwarp/blob/master/src/main/java/net/imglib2/realtransform/Wrapped2DTransformAs3D.java
 *
 */
public class InvertibleWrapped2DTransformAs3D implements InvertibleRealTransform
{
    public InvertibleRealTransform transform;

    public double[] tmp;

    public InvertibleWrapped2DTransformAs3D(final InvertibleRealTransform transform )
    {
        this.transform = transform;
        tmp = new double[ 2 ];
    }

    public InvertibleRealTransform getTransform()
    {
        return transform;
    }

    @Override
    public int numSourceDimensions()
    {
        return 3;
    }

    @Override
    public int numTargetDimensions()
    {
        return 3;
    }

    @Override
    public void apply( double[] source, double[] target )
    {
        // TODO this could be done without tmp if all downstream implementations
        // could take source and target inputs with dim larger than the
        // transform dim
        tmp[ 0 ] = source[ 0 ];
        tmp[ 1 ] = source[ 1 ];
        transform.apply( tmp, tmp );
        target[ 0 ] = tmp[ 0 ];
        target[ 1 ] = tmp[ 1 ];
//		transform.apply( source, target );
        target[ 2 ] = source[ 2 ];
    }

    @Override
    public void apply( RealLocalizable source, RealPositionable target )
    {
        // TODO this could be done without tmp if all downstream implementations
        // could take source and target inputs with dim larger than the
        // transform dim
        tmp[ 0 ] = source.getDoublePosition( 0 );
        tmp[ 1 ] = source.getDoublePosition( 1 );
        transform.apply( tmp, tmp );
        target.setPosition( tmp[ 0 ], 0 );
        target.setPosition( tmp[ 1 ], 1 );
//		transform.apply( source, target );
        target.setPosition( source.getDoublePosition( 2 ), 2 );
    }

    @Override
    public void applyInverse( double[] source, double[] target )
    {
        tmp[ 0 ] = target[ 0 ];
        tmp[ 1 ] = target[ 1 ];
        transform.applyInverse( tmp, tmp );
        source[ 0 ] = tmp[ 0 ];
        source[ 1 ] = tmp[ 1 ];
//		transform.applyInverse( source, target );
        source[ 2 ] = target[ 2 ];
    }

    @Override
    public void applyInverse(RealPositionable source, RealLocalizable target )
    {
        tmp[ 0 ] = target.getDoublePosition( 0 );
        tmp[ 1 ] = target.getDoublePosition( 1 );
        transform.applyInverse( tmp, tmp );
        source.setPosition( tmp[ 0 ], 0 );
        source.setPosition( tmp[ 1 ], 1 );
//		transform.applyInverse( source, target );
        source.setPosition( target.getDoublePosition( 2 ), 2 );
    }

    public InvertibleRealTransform copy()
    {
        return new InvertibleWrapped2DTransformAs3D( transform.copy() );
    }

    @Override
    public InvertibleRealTransform inverse()
    {
        return new InvertibleWrapped2DTransformAs3D( transform.inverse() );
    }

}