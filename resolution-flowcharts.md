# Flowcharts de Decisão — Resolution Cascade, Registry e Developer Workflow

Fontes PlantUML para os 4 flowcharts recomendados para enriquecer a dissertação (ver
`thesis/chapter3.tex` e `thesis/chapter4.tex`). Ao contrário dos diagramas em
`aws-lambda-implementation-diagrams.md` (arquitectura, classes, sequência), estes são *activity
diagrams* focados na lógica de decisão em si — os ramos condicionais que hoje só existem em prosa.

Renderizar com a extensão PlantUML do VS Code, `plantuml.com/plantuml`, ou
`java -jar plantuml.jar resolution-flowcharts.md` (requer Graphviz).

---

## 1. Resolution Cascade

Ilustra `thesis/chapter3.tex` §3.1 "The Resolution Cascade" e
`thesis/chapter4.tex` §4.1 "Deployment-Time Endpoint Resolution" — o algoritmo de 3 níveis que
resolve um `internalFunction` call, parando no primeiro nível que tiver sucesso. Os ramos de
`ambiguous reference` e `insufficient permissions` são omitidos aqui por clareza e detalhados no
flowchart 3 (Validation & Error Semantics).

```plantuml
@startuml resolution-cascade
title Resolution Cascade — resolving an internalFunction call

start
:internalFunction("functionRef"[, "region"])\nintercepted at deploy time;

:Level 1 — Registry lookup\n(exact match, then suffix match "region/functionRef");
if (registry hit?) then (yes)
  :validate stored endpoint\nagainst live resource\n(Cloud Run service / Lambda function);
  if (live resource still matches\nstored endpoint?) then (unchanged)
    #51cf66:use stored endpoint;
    stop
  elseif (endpoint changed) then (yes)
    :refresh registry entry;
    #51cf66:use refreshed endpoint;
    stop
  else (resource no longer exists)
    #ff6b6b:remove stale entry\nabort deployment;
    stop
  endif
else (miss)
  :Level 2 — Provider discovery\n(cross-region lookup on GCP/AWS,\nor single call if "region/functionRef" given);
  if (found on provider?) then (yes)
    :register discovered metadata\n(no deployment triggered);
    #51cf66:use discovered endpoint;
    stop
  else (miss)
    :Level 3 — QuickFaaS deployment;
    if (deploymentDescriptorPath\nsupplied on the call?) then (yes)
      :invoke QuickFaaS to deploy the function;
      :register returned metadata;
      #51cf66:use newly deployed endpoint;
      stop
    else (no)
      #ff6b6b:fail-fast\n(diagnostic names the unresolved function);
      stop
    endif
  endif
endif

note right
  Ambiguous references and authorization
  failures short-circuit this cascade at
  Level 1/2 — see flowchart 3 (Validation &
  Error Semantics) for those branches.
end note
@enduml
```

---

## 2. Registry Bootstrap, Creation and Drift

Ilustra `thesis/chapter4.tex` §4.1 "Creation, Bootstrap and Updates" — hoje uma lista de 4 bullets
que descreve, na prática, 3 decisões distintas disparadas por eventos diferentes.

```plantuml
@startuml registry-bootstrap
title Function Registry — bootstrap, automatic registration and drift handling

start
:OmniFlow needs to read/write\nthe Function Registry;

if (registry file exists?) then (no)
  :bootstrap by discovery —\nlist Cloud Run services (GCP) /\nLambda functions across every region (AWS);
  :promote names seen in >1 region\nto "region/name" keys;
  :write one entry per discovered function;
  #51cf66:registry created;
  stop
else (yes)
  if (functionRef already\nin the registry?) then (yes — Level 1 hit)
    :validate stored endpoint\nagainst the live resource;
    if (endpoint unchanged?) then (yes)
      :use as-is;
      stop
    elseif (endpoint changed) then (yes)
      :refresh entry\n(drift handling);
      :use refreshed endpoint;
      stop
    else (resource no longer exists)
      #ff6b6b:remove stale entry\nabort deployment;
      stop
    endif
  else (no — resolved via Level 2/3)
    :write new entry with\ndiscovered/deployed metadata;
    :update top-level "updatedAt" timestamp;
    #51cf66:registry updated;
    stop
  endif
endif
@enduml
```

---

## 3. Validation & Error Semantics

Ilustra `thesis/chapter4.tex` §4.1 "Validation and Error Semantics" — os 4 modos de falha,
cada um com condição de despoletamento e recuperabilidade distintas. Note que só
`UNRESOLVABLE FUNCTION` é recuperável fornecendo um `deploymentDescriptorPath`.

```plantuml
@startuml validation-error-semantics
title Endpoint Resolution — validation and error semantics

start
switch (failure trigger?)

case ( absent from both\nregistry and provider )
  if (deploymentDescriptorPath\nsupplied?) then (yes)
    :proceed to Level 3 deploy\n(not a failure — see flowchart 1);
  else (no)
    #ff6b6b:UNRESOLVABLE FUNCTION\nabort; diagnostic names the function\nand the two corrective actions\n(deploy it first, or add a descriptor);
  endif

case ( functionRef matches\n>1 region, in the registry\nor on the provider )
  #ff6b6b:AMBIGUOUS REFERENCE\nabort; ask developer to use\n"region/functionRef";\n(never rescued by a descriptor —\ndata-consistency problem, not absence);

case ( registry hit, but live lookup\nfinds no matching resource\nafter a cross-region re-check )
  #ff6b6b:STALE REGISTRY ENTRY\nremove entry; abort deployment\nrather than emit a dead binding;

case ( live lookup returns an\nauthorization failure, not "absent" )
  #ff6b6b:INSUFFICIENT PERMISSIONS\nabort immediately; no rediscovery\nattempted (a refusal gives no\ninformation about existence);

endswitch
stop
@enduml
```

---

## 4. Developer Procedure — Integrated Deployment Walkthrough

Ilustra `thesis/chapter3.tex` §3.3 "Developer Procedure for Deploying a Workflow" e
§3.3.1 "Walkthrough of an Integrated Deployment". Usa swimlanes para separar o que o developer
prepara do que o OmniFlow resolve automaticamente em tempo de deploy — reforçando o argumento de
que a integração elimina passos manuais.

```plantuml
@startuml developer-walkthrough
title Developer procedure and integrated deployment walkthrough

|Developer|
start
:write workflow DSL;
if (call type?) then (internal function)
  :internalFunction("functionRef", "descriptor.json");
  :prepare QuickFaaS deployment descriptor\n+ implementation file (functionFile);
else (external endpoint)
  :host(...) / path(...)\nwith a literal endpoint;
endif
:run integrated deploy;

|OmniFlow (deploy time)|
:traverse the workflow tree\n(nested branches, iterations, parallel blocks);
if (call carries an\ninternalFunction declaration?) then (no — external)
  :leave call unchanged;
  note right: not stored in the registry
else (yes — internal)
  :run the Resolution Cascade;
  note right: see flowchart 1
  if (cascade succeeded?) then (yes)
    :resolved endpoint injected\ninto the call step;
  else (no)
    #ff6b6b:one of the 4 failure modes\n(see flowchart 3) — abort before render;
    stop
  endif
endif

if (every call in the workflow\nnow resolved?) then (yes)
  :render provider-specific artifact\n(Step Functions JSON / Cloud Workflows YAML);
  #51cf66:deploy the workflow;
  stop
endif
@enduml
```
