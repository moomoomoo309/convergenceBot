### TODO

## Writing the core

## Stage 1
- Figure out dynamic class loading in kotlin - Partially implemented
- Write up interface for each chat protocol - Implemented

## Stage 2
- Implement Command registration - Implemented
    - Allow commands to be limited by protocol if necessary
- Implement alias registration - Implemented.
    - Make sure aliases can only execute commands they have access to in their scope (it must be supported on the protocol)
        - Implemented because of how chats work.

## Writing the bots

## Stage 3
- Implement bot for each chat protocol
- Make bot implement the core interface

## Stage 4
- Add richer integration with individual protocols

## Stage 5+
- Go back to stage 3 with a different protocol

