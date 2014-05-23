# CarChase 2: text-generating component

The patterns for generating sentences or concatenations are parsed from a file. This
file contains the definition of patterns. A pattern defines _many_ situations.
Each pattern has a situation type and defines conditions and some message 
templates. A message template has an information level, one message type for both, 
the beginning and the ending and the message, which can contains variables. A 
variable is replaced at the runtime with the actual content, e.g. a `street` variable 
will be replaced with the current street. At the runtime, on every update the 
patterns, where all conditions are met, are determined and one message with a fitting
type and information level will be chosen for the output.

As not all situations can be defined by a pattern, some situations are parsed 
from a `messages.txt` file. It specifies situations which cannot be put in 
general rules. Similar to the patterns, a situations has conditions and specifies
some messages.

If there is a matching situation from the messages file, these will be preferred
over automatically generated situations.

## Situations
Currently, there is only one situation type, more are coming in future. (Planned
    situations are italic.) Every situation has an "optional" flag.

| Type | Parameters |
| ---- | ---------- |
| Driving | Street, Previous-Street, Point, Distance to point, Direction, Previous-Direction |
| _Beginning_ | Street, Point, Distance to Point, Speed |
| _Path_ | Current Street, Previous Street 1, Previous Street 2, ... |
| _Speed_ | Street, Speed, Old-Speed |

## Patterns
As mentioned above, there are variables, that will be replaced at runtime. Every pattern
has an optional flag.
Here's a list of 
(_upcoming_) variables:

| Name | Replacement |
| ---- | ----------- |
| intstreet | internal name of the current street |
| intprevstreet | internal name of the previous street |
| intjunctionstreet | internal name of the junction street |
| street | name of the current street |
| prevstreet | name of the previous street |
| junctionstreet | name of the junction street |
| flex1street | variant of the current street name, to match with preposition "in" |
| flex1prevstreet | variant of the previous street name, to match with preposition "in" |
| flex1junctionstreet | variant of the junction street name, to match with preposition "in" |
| flex2street | variant of the current street name, to match with preposition "out of" |
| flex2prevstreet | variant of the previous street name, to match with preposition "out of" |
| flex2junctionstreet | variant of the junction street name, to match with preposition "out of" |
| distance | distance to the next point _Only for usage in conditions!_ |
| direction | current direction |
| prevdirection | previous direction |
| bidirectional | whether the street is bidirectional (0 = no, 1 = yes) |
| pointname | name of the next point |
| numstreets | number of streets that cross the next point |
| isjunction | whether the next point is a junction (0 = no, 1 = yes, 2 = T-junction) |
| wasjunction | whether the last point was a junction (0 = no, 1 = yes, 2 = T-junction) |
| speed | current speed |
| _prevspeed_ | previous speed |
| _angle_ | The angle between the current and the previous street |
| _leftright_ | after a junction: whether the car turns left or right |

## Information level
Messages with a higher information level usually take
more time. Therefore, if the car has a high speed, the messages with a lower
information level will be chosen, whereas messages with a higher information
level will be chosen, if the car has a lower speed. (Levels specified in
    CaChaseTTS.MessageInformationLevel)

## Type
A type has a number and a flag, which indicates the message to require another
sentence. If the message requires another sentence at the beginning or ending,
the number of the previous or following message has to match. (Message types are
    specified in CarChaseTTS.MessageType)

## Dispatching TTS actions
On every update of the car's position, the dispatch thread will be notified
check whether a situation matches and to dispatch it. If there are two or more
situations matching, the first, which offers a continuation to the current
sentence will be chosen. If there are upcoming sentences, and the duration of
these sentences is longer than 2 seconds, all optional sentences will be
revoked. _In future, continuations will also be appended, if there is a 
break of some seconds. Furthermore, if a text is revoked, it will be replaced
with one message of a lower information level, which is designed to be shorter._
