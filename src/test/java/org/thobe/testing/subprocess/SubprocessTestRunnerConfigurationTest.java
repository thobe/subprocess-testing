package org.thobe.testing.subprocess;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(SubprocessTestRunner.class)
@SubprocessTestRunner.SubprocessConfiguration(SubprocessTestRunnerConfigurationTest.Configuration.class)
public class SubprocessTestRunnerConfigurationTest
{
    private static final String VALUE = "the_test_works";
    private static final String KEY = SubprocessTestRunnerConfigurationTest.class.getName();

    public static class Configuration implements SubprocessConfigurator
    {
        @Override
        public <T extends SubprocessConfiguration<T>> void configureProcess( T starter )
        {
            starter.vmArg( "-D" + KEY + "=" + VALUE );
        }
    }

    @Test
    public void shouldHaveConfiguredProperty() throws Exception
    {
        assertEquals( VALUE, System.getProperty( KEY ) );
    }
}
