import org.junit.Assert;
import org.junit.Test

import es.eci.utils.ParameterValidator;

class TestParameterValidator {

	@Test
	public void testValidationOK() {
		// Validación correcta
		ParameterValidator.Builder builder = ParameterValidator.builder();
		
		ParameterValidator validator = 
			builder
				.add("param1", "aldfsdf")
				.add("param2", 234342)
				.add("param3", new Date())
				.build();
		try {
			validator.validate();
		}
		catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}
	
	@Test
	public void testDefaultValidationFail() {
		// Fallo en validación
		ParameterValidator.Builder builder = ParameterValidator.builder();
		
		ParameterValidator validator = 
			builder
				.add("param1", "aldfsdf")
				.add("param2", null)
				.add("param3", new Date())
				.add("param4", null)
				.build();
		try {
			validator.validate();
			Assert.fail();
		}
		catch(Exception e) {
			// El error tiene que tener param2 y param4
			String message = e.getMessage();
			System.err.println(message);
			Assert.assertTrue(message.contains("param2"));
			Assert.assertTrue(message.contains("param4"));
		}
	}
	
	@Test 
	public void testCustomValidationOK() {
		ParameterValidator.Builder builder = ParameterValidator.builder();
		
		ParameterValidator validator = 
			builder
				// No puede tener la letra e
				.add("param1", "aldfsdf", { it -> return !it.contains("e") })
				.add("param2", 2342, { it -> return it > 500 && it < 5000 })
				.add("param3", new Date())
				.build();
		try {
			validator.validate();
		}
		catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}
	
	@Test 
	public void testCustomValidationFail() {
		ParameterValidator.Builder builder = ParameterValidator.builder();
		
		ParameterValidator validator = 
			builder
				// No puede tener la letra e
				.add("param1", "aldfesdf", { it -> return !it.contains("e") })
				.add("param2", 23420, { it -> return it > 500 && it < 5000 })
				.add("param3", new Date())
				.build();
		try {
			validator.validate();
			Assert.fail();
		}
		catch (Exception e) {
			// El error tiene que tener param1 y param2
			String message = e.getMessage();
			System.err.println(message);
			Assert.assertTrue(message.contains("param1"));
			Assert.assertTrue(message.contains("param2"));
		}
	}
}
