package com.rescuegrid.algorithm;

import com.rescuegrid.model.*;
import java.util.*;

public interface Router {
    Route findRoute(City city, Node from, Node to);
    String getName();
}
