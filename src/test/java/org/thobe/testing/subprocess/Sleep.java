package org.thobe.testing.subprocess;

class Sleep
{
    public static void main( String[] args ) throws Exception
    {
        for ( String arg : args )
        {
            Thread.sleep( Integer.parseInt( arg ) );
        }
    }
}
