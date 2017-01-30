import org.junit.Assert;
import org.junit.Test

import components.MavenComponent

import es.eci.utils.pom.SortGroupsStrategy

/**
 * Esta clase hace las pruebas unitarias de la agrupación de jobs para ordenación. 
 */
class TestGroupsSorting {

	@Test
	public void testTrivialNonGrouping() {
		// Orden: C1 <- C2 <- C3
		// Dependencias:
		//	MavenComponent.dependsOn(C2, C1) == true
		//	MavenComponent.dependsOn(C3, C2) == true
		// Debe devolver los grupos
		// 	g1:C1
		// 	g2:C2
		//	g3:C3
		MavenComponent C1 = new MavenComponent("C1");
		MavenComponent C2 = new MavenComponent("C2");
		C2.addDependency(C1);
		MavenComponent C3 = new MavenComponent("C3");
		C3.addDependency(C2);
		List<MavenComponent> components = [ C1, C2, C3 ]
		List<List<MavenComponent>> groups = 
			new SortGroupsStrategy().sortGroups(components);
		// Aserción de los resultados
		Assert.assertEquals(3, groups.size());
		Assert.assertEquals(1, groups[0].size());
		Assert.assertEquals(1, groups[1].size());
		Assert.assertEquals(1, groups[2].size());
		Assert.assertTrue(groups[0].contains(C1));
		Assert.assertTrue(groups[1].contains(C2));
		Assert.assertTrue(groups[2].contains(C3));
	}
	
	@Test
	public void testTrivialGrouping() {
		// Orden: C1 <- C2 <- C3
		// Dependencias:
		//	MavenComponent.dependsOn(C2, C1) == true
		//	MavenComponent.dependsOn(C3, C1) == true
		// Debe devolver los grupos
		// 	g1:C1
		// 	g2:C2,C3
		MavenComponent C1 = new MavenComponent("C1");
		MavenComponent C2 = new MavenComponent("C2");
		C2.addDependency(C1);
		MavenComponent C3 = new MavenComponent("C3");
		C3.addDependency(C1);
		List<MavenComponent> components = [ C1, C2, C3 ]
		List<List<MavenComponent>> groups =
			new SortGroupsStrategy().sortGroups(components);
		// Aserción de los resultados
		Assert.assertEquals(2, groups.size());
		Assert.assertEquals(1, groups[0].size());
		Assert.assertEquals(2, groups[1].size());
		Assert.assertTrue(groups[0].contains(C1));
		Assert.assertTrue(groups[1].contains(C2));
		Assert.assertTrue(groups[1].contains(C3));
	}
	
	@Test
	public void testComplexGrouping() {
		// Orden: C1 -> C2 -> C3 -> C4 -> C5
		// Dependencias:
		//	MavenComponent.dependsOn(C2, C1) == false
		//	MavenComponent.dependsOn(C3, C2) == true
		//	MavenComponent.dependsOn(C4, C2) == true
		//	MavenComponent.dependsOn(C4, C3) == false
		//	MavenComponent.dependsOn(C5, C3) == true
		// Debe devolver los grupos
		// 	g1: C1, C2
		// 	g2: C3, C4
		// 	g3: C5
		MavenComponent C1 = new MavenComponent("C1");
		MavenComponent C2 = new MavenComponent("C2");
		MavenComponent C3 = new MavenComponent("C3");
		C3.addDependency(C2);
		MavenComponent C4 = new MavenComponent("C4");
		C4.addDependency(C2);
		MavenComponent C5 = new MavenComponent("C5");
		C5.addDependency(C3);
		List<MavenComponent> components = [ C1, C2, C3, C4, C5 ]
		List<List<MavenComponent>> groups =
			new SortGroupsStrategy().sortGroups(components);
		// Aserción de los resultados
		Assert.assertEquals(3, groups.size());
		Assert.assertEquals(2, groups[0].size());
		Assert.assertEquals(2, groups[1].size());
		Assert.assertEquals(1, groups[2].size());
		Assert.assertTrue(groups[0].contains(C1));
		Assert.assertTrue(groups[0].contains(C2));
		Assert.assertTrue(groups[1].contains(C3));
		Assert.assertTrue(groups[1].contains(C4));
		Assert.assertTrue(groups[2].contains(C5));
	}
	
	@Test 
	public void testSingleDependencyGrouping() {
		// Orden: C1 -> C2 -> C3 -> C4 -> C5 -> C6 -> C7 -> C8 -> C9 -> C10
		// Dependencias:
		//	MavenComponent.dependsOn(C2, C1) == true
		//	MavenComponent.dependsOn(C3, C1) == true
		//	MavenComponent.dependsOn(C4, C1) == true
		//	MavenComponent.dependsOn(C5, C1) == true
		//	MavenComponent.dependsOn(C6, C1) == true
		//	MavenComponent.dependsOn(C7, C1) == true
		//	MavenComponent.dependsOn(C8, C1) == true
		//	MavenComponent.dependsOn(C9, C1) == true
		//	MavenComponent.dependsOn(C10, C1) == true
		// Debe devolver los grupos
		// 	g1: C1
		// 	g2: C2,C3,C4,C5,C6,C7,C8,C9,C10
		MavenComponent C1 = new MavenComponent("C1");
		MavenComponent C2 = new MavenComponent("C2");
		C2.addDependency(C1);
		MavenComponent C3 = new MavenComponent("C3");
		C3.addDependency(C1);
		MavenComponent C4 = new MavenComponent("C4");
		C4.addDependency(C1);
		MavenComponent C5 = new MavenComponent("C5");
		C5.addDependency(C1);
		MavenComponent C6 = new MavenComponent("C6");
		C6.addDependency(C1);
		MavenComponent C7 = new MavenComponent("C7");
		C7.addDependency(C1);
		MavenComponent C8 = new MavenComponent("C8");
		C8.addDependency(C1);
		MavenComponent C9 = new MavenComponent("C9");
		C9.addDependency(C1);
		MavenComponent C10 = new MavenComponent("C10");
		C10.addDependency(C1);
		List<MavenComponent> components = 
			[ C1, C2, C3, C4, C5, C6, C7, C8, C9, C10 ];
		List<List<MavenComponent>> groups =
			new SortGroupsStrategy().sortGroups(components);
		// Validar aserciones
		Assert.assertEquals(2, groups.size());
		Assert.assertEquals(1, groups[0].size());
		Assert.assertEquals(9, groups[1].size());
		Assert.assertTrue(groups[0].contains(C1));
		Assert.assertTrue(groups[1].contains(C2));
		Assert.assertTrue(groups[1].contains(C3));
		Assert.assertTrue(groups[1].contains(C4));
		Assert.assertTrue(groups[1].contains(C5));
		Assert.assertTrue(groups[1].contains(C6));
		Assert.assertTrue(groups[1].contains(C7));
		Assert.assertTrue(groups[1].contains(C8));
		Assert.assertTrue(groups[1].contains(C9));
		Assert.assertTrue(groups[1].contains(C10));
	}
	
	@Test
	public void testVeryComplexGrouping() {
		// Orden: C1 -> C2 -> C3 -> C4 -> C5 -> C6 -> C7 -> C8 -> C9 -> C10
		// Dependencias:
		//	MavenComponent.dependsOn(C2, C1) == true
		//	MavenComponent.dependsOn(C3, C1) == true
		//	MavenComponent.dependsOn(C4, C1) == true
		//	MavenComponent.dependsOn(C6, C5) == true
		//	MavenComponent.dependsOn(C7, C5) == true
		//	MavenComponent.dependsOn(C8, C5) == true
		//	MavenComponent.dependsOn(C9, C5) == true
		// Debe devolver los grupos
		// 	g1: C1
		// 	g2: C2,C3,C4,C5
		//	g3: C6,C7,C8,C9,C10
		MavenComponent C1 = new MavenComponent("C1");
		MavenComponent C2 = new MavenComponent("C2");
		C2.addDependency(C1);
		MavenComponent C3 = new MavenComponent("C3");
		C3.addDependency(C1);
		MavenComponent C4 = new MavenComponent("C4");
		C4.addDependency(C1);
		MavenComponent C5 = new MavenComponent("C5");
		MavenComponent C6 = new MavenComponent("C6");
		C6.addDependency(C5);
		MavenComponent C7 = new MavenComponent("C7");
		C7.addDependency(C5);
		MavenComponent C8 = new MavenComponent("C8");
		C8.addDependency(C5);
		MavenComponent C9 = new MavenComponent("C9");
		C9.addDependency(C5);
		MavenComponent C10 = new MavenComponent("C10");
		List<MavenComponent> components =
			[ C1, C2, C3, C4, C5, C6, C7, C8, C9, C10 ];
		List<List<MavenComponent>> groups =
			new SortGroupsStrategy().sortGroups(components);
		// Validar aserciones
		Assert.assertEquals(3, groups.size());
		Assert.assertEquals(1, groups[0].size());
		Assert.assertEquals(4, groups[1].size());
		Assert.assertEquals(5, groups[2].size());
		Assert.assertTrue(groups[0].contains(C1));
		Assert.assertTrue(groups[1].contains(C2));
		Assert.assertTrue(groups[1].contains(C3));
		Assert.assertTrue(groups[1].contains(C4));
		Assert.assertTrue(groups[1].contains(C5));
		Assert.assertTrue(groups[2].contains(C6));
		Assert.assertTrue(groups[2].contains(C7));
		Assert.assertTrue(groups[2].contains(C8));
		Assert.assertTrue(groups[2].contains(C9));
		Assert.assertTrue(groups[2].contains(C10));
	}
}
