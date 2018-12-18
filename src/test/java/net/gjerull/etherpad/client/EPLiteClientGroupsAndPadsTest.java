package net.gjerull.etherpad.client;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.graphwalker.core.machine.ExecutionContext;

import org.graphwalker.core.machine.ExecutionContext;
import org.graphwalker.java.annotation.Edge;
import org.graphwalker.java.annotation.GraphWalker;

@GraphWalker(value = "random(edge_coverage(100))", start = "setUp")
public class EPLiteClientGroupsAndPadsTest extends ExecutionContext implements IEPLiteClientGroups {
    //public final static Path MODEL_PATH = Paths.get(".graphml");

	@Override
	public void createGroup() {
		// TODO Auto-generated method stub
		System.out.println("Creando grupo");
	}

	@Override
	public void GroupDoesNotExist() {
		// TODO Auto-generated method stub
		System.out.println("El grupo no existe");
	}

	@Override
	public void GroupExists() {
		// TODO Auto-generated method stub
		System.out.println("El grupo existe");
	}

	@Override
	public void createGroupPad() {
		// TODO Auto-generated method stub
		System.out.println("Creando un pad dentro de un grupo");
	}

	@Override
	public void deleteGroup() {
		// TODO Auto-generated method stub
		System.out.println("Eliminando el grupo");
	}

	@Override
	public void listAllGroups() {
		// TODO Auto-generated method stub
		System.out.println("Listando todos los grupos");
	}

	@Override
	public void NoExistePad() {
		System.out.println("El pad no existe");
	}

	@Override
	public void getText() {
		System.out.println("Leyendo el pad");
	}

	@Override
	public void deletePad() {
		System.out.println("Borrando el pad");
	}

	@Override
	public void ExistePad() {
		System.out.println("El pad existe");
	}

	@Override
	public void setUp() {
		System.out.println("Setting up");
	}

	@Override
	public void createPad() {
		System.out.println("Creando el pad");
	}

	@Override
	public void setText() {
		System.out.println("Escribiendo en el pad");
	}

}
