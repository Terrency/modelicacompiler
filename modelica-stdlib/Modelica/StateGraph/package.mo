within Modelica;
package StateGraph "Library for state graphs and finite state machines"
  extends Modelica.Icons.Package;

  annotation (
    Documentation(info="<html>
<p>
This library contains components for modeling discrete event and
reactive systems using state graphs (finite state machines).
</p>
</html>"));

  package Interfaces "Connectors and partial models for state graphs"
    extends Modelica.Icons.InterfacesPackage;

    connector StepPort "Port of a step"
      output Boolean active "True if step is active";
      input Boolean fire "True if transition fires";
    end StepPort;

    connector TransitionPort "Port of a transition"
      input Boolean active "True if preceding step is active";
      output Boolean fire "True if transition fires";
    end TransitionPort;

    partial block PartialStep "Partial step model"
      extends Modelica.Icons.Block;

      parameter Boolean initialStep = false "True if this is an initial step";
      parameter Integer nIn = 1 "Number of input connections";
      parameter Integer nOut = 1 "Number of output connections";

      Boolean active "True if step is active";
      Boolean fire[nOut] "True if one of the output transitions fires";

      StepPort inPort[nIn];
      StepPort outPort[nOut];

    protected
      Boolean newActive;

    initial equation
      active = initialStep;

    equation
      // Input connections
      for i in 1:nIn loop
        inPort[i].active = active;
      end for;

      // Output connections
      for i in 1:nOut loop
        outPort[i].active = active;
        fire[i] = outPort[i].fire;
      end for;

      // State transition
      newActive = (active and not any(fire[1:nOut])) or
                  (not active and any(inPort[1:nIn].fire));

      active = pre(newActive);

    end PartialStep;

    partial block PartialTransition "Partial transition model"
      extends Modelica.Icons.Block;

      parameter Boolean enableTimer = false "True if timer is enabled";
      parameter Modelica.SIunits.Time waitTime = 0 "Wait time before transition";

      Boolean fire "True if transition fires";
      Boolean condition "Transition condition";

      TransitionPort inPort;
      TransitionPort outPort;

    protected
      Modelica.SIunits.Time tActivated "Time when step was activated";

    equation
      inPort.fire = fire;
      outPort.fire = fire;

      // Fire when condition is true and preceding step is active
      fire = inPort.active and condition and
             (if enableTimer then time >= tActivated + waitTime else true);

    initial equation
      tActivated = time;

    equation
      when inPort.active then
        tActivated = time;
      end when;

    end PartialTransition;

  end Interfaces;

  package Blocks "State graph blocks"
    extends Modelica.Icons.Package;

    block Step "Step of a state graph"
      extends Interfaces.PartialStep;

      output Boolean y = active "Output true when step is active";

    equation
      // Default condition: step remains active until a transition fires
      // This is handled by the partial model

    end Step;

    block InitialStep "Initial step of a state graph"
      extends Step(initialStep = true);
    end InitialStep;

    block Transition "Transition between steps"
      extends Interfaces.PartialTransition;

      Modelica.Blocks.Interfaces.BooleanInput conditionInput
        "External condition input";

    equation
      condition = conditionInput;

    end Transition;

    block TransitionWithTimer "Transition with timer"
      extends Interfaces.PartialTransition(enableTimer = true);

      Modelica.Blocks.Interfaces.BooleanInput conditionInput
        "External condition input";

    equation
      condition = conditionInput;

    end TransitionWithTimer;

    block Alternative "Alternative branching"
      extends Modelica.Icons.Block;

      parameter Integer nBranches = 2 "Number of alternative branches";

      Modelica.Blocks.Interfaces.BooleanInput conditions[nBranches]
        "Branch conditions";
      Modelica.Blocks.Interfaces.IntegerOutput selectedBranch
        "Selected branch (1..nBranches)";

    equation
      selectedBranch = if conditions[1] then 1 else
                       if conditions[2] then 2 else
                       if nBranches > 2 and conditions[3] then 3 else
                       if nBranches > 3 and conditions[4] then 4 else 1;

    end Alternative;

    block Parallel "Parallel branching"
      extends Modelica.Icons.Block;

      parameter Integer nBranches = 2 "Number of parallel branches";

      Modelica.Blocks.Interfaces.BooleanOutput activate[nBranches]
        "Activate parallel branches";

    equation
      for i in 1:nBranches loop
        activate[i] = true;
      end for;

    end Parallel;

  end Blocks;

  package Examples "State graph examples"
    extends Modelica.Icons.ExamplesPackage;

    model SimpleStateGraph "Simple state graph example"
      extends Modelica.Icons.Example;

      InitialStep step1;
      Step step2;
      Step step3;

      Transition t1(conditionInput = true);
      Transition t2(conditionInput = true);
      Transition t3(conditionInput = true);

    equation
      connect(step1.outPort[1], t1.inPort);
      connect(t1.outPort, step2.inPort[1]);
      connect(step2.outPort[1], t2.inPort);
      connect(t2.outPort, step3.inPort[1]);
      connect(step3.outPort[1], t3.inPort);
      connect(t3.outPort, step1.inPort[1]);

    end SimpleStateGraph;

  end Examples;

end StateGraph;