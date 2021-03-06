package datawave.common.cl;

import org.apache.commons.cli.Option;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class OptionBuilderTest {
    
    private OptionBuilder optionBuilder;
    
    @Before
    public void setup() {
        this.optionBuilder = new OptionBuilder();
        this.optionBuilder.reset();
    }
    
    @Test
    public void testCreate() throws Exception {
        Option option = this.optionBuilder.create("opt", "desc");
        assertEquals(option.getArgs(), 0);
        assertEquals(option.getType(), null);
        assertEquals(option.getOpt(), "opt");
        assertEquals(option.getDescription(), "desc");
        assertEquals(option.getLongOpt(), null);
        assertFalse(option.isRequired());
        assertEquals(option.getValueSeparator(), 0);
    }
    
    @Test
    public void testCreate1() throws Exception {
        Option option = this.optionBuilder.create("opt", "longOpt", "desc");
        assertEquals(option.getLongOpt(), "longOpt");
    }
}
