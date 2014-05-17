# CarChase 2 TTS

The situations for the TTS are parsed from a `messages.txt` file. It specifies the
situations where a TTS action is triggered. A situation specifies some messages.
Each message has an unique ID, an information level, and one message types for
each, the beginning and the end of the sentence.

## Situation
Currently, there is only one situation type, more are coming in future. (Planned
    situations are italic.) Every situation has an "optional" flag.

| Type | Parameters |
| ---- | ---------- |
| Driving | Street, Previous-Street, Point, Distance to point, Direction, Previous-Direction |
| _Beginning_ | Street, Point, Distance to Point, Speed |
| _Path_ | Current Street, Previous Street 1, Previous Street 2, ... |
| _Speed_ | Street, Speed, Old-Speed |

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
revoked.
