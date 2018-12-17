// Generated by GraphWalker (http://www.graphwalker.org)
package net.gjerull.etherpad.client;

import org.graphwalker.java.annotation.Model;
import org.graphwalker.java.annotation.Vertex;
import org.graphwalker.java.annotation.Edge;

@Model(file = "net/gjerull/etherpad/client/EPLiteClientGroupsAndPads.graphml")
public interface IEPLiteClientGroups {

    @Edge()
    void createGroup();

    @Vertex()
    void NoExistePad();

    @Edge()
    void getText();

    @Vertex()
    void GroupDoesNotExist();

    @Vertex()
    void GroupExists();

    @Edge()
    void deletePad();

    @Vertex()
    void ExistePad();

    @Edge()
    void setUp();

    @Edge()
    void createGroupPad();

    @Edge()
    void deleteGroup();

    @Edge()
    void listAllGroups();

    @Edge()
    void createPad();

    @Edge()
    void setText();
}
