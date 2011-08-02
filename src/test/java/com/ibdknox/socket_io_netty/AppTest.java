package com.ibdknox.socket_io_netty;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue( true );
    }

    public void testDecode()
    {
        String msg = "~m~16~m~{\n user : {\n  }\n}";
        String decode = com.ibdknox.socket_io_netty.SocketIOUtils.decode(msg);
        assertTrue( decode.equals("{\n user : {\n  }\n}") );
    }
}
