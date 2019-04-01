/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hypo_mtops;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author youda
 */
public class HYPO_MTOPS {
    public static final long 
            // Status codes
            OK = 0,
            
            // Reasons for process to give up CPU
            PROCESS_HALTED = 1,
            TIME_SLICE_EXPIRED = 2,
            START_OF_INPUT_OPERATION = 3,
            START_OF_OUTPUT_OPERATION = 4,
            
            // System shutdown Status
            SYSTEM_SHUTDOWN = 5,
            
            // Error status codes
            ER_FILE_NOT_FOUND = -1,
            ER_ADDRESS_OUT_OF_RANGE = -2,
            ER_NO_END_OF_PROGRAM = -3,
            ER_INVALID_OP_CODE = -4,
            ER_INVALID_OP_MODE = -5,
            ER_INVALID_GPR = -6,
            ER_INVALID_PC_VALUE = -7,
            ER_FATAL_RUNTIME_ERROR = -8,
            ER_STACK_OVERFLOW = -9,
            ER_STACK_UNDERFLOW = -10,
            ER_NO_FREE_MEMORY = -11,
            ER_INVALID_MEMORY_SIZE = -12,
            ER_INVALID_MEMORY_ADDRESS = -13,
            ER_INVALID_INTERRUPT_ID = -14,
            
            // Program memory Specifications
            PROGRAM_MEM_START_ADDR = 0,
            PROGRAM_MEM_SIZE = 4000,
            
            // User memory specifications
            USER_MEM_START_ADDR = 4000,
            USER_MEM_SIZE = 3000,
            
            // OS memory specifications
            OS_MEM_START_ADDR = 7000,
            OS_MEM_SIZE = 3000,
            
            // End of list value is invalid memory address
            
            // I don't think End of List should be a single value set to -1
            // I think it should point to the beginning of the Free block
            // and there should be a different one of USer Mem List Free block and OS Mem List Free Block, 
            // check allocateOSMem for why - Edward
            END_OF_LIST = -1,
            
            // Stack size is always 10 in this implementation
            STACK_SIZE = 10,
            
            // CPU clock cycles alloted
            TIME_SLICE = 200,
            
            // Size of memory and GPRs in words
            MEMORY_SIZE = 10000,
            GPR_SIZE = 8,
            
            // Below are the descriptors for the elements stored in a PCB.
            // Each descriptor is set to the 'shift' amount from PCB starting
            // address needed to access its corresponding value
            NEXT_PCB_IDX = 0,
            PID_IDX = 1,
            PROCESS_STATE_IDX = 2,
            REASON_FOR_WAITING_IDX = 3,
            PRIORITY_IDX = 4,
            STACK_START_ADDR_IDX = 5,
            STACK_SIZE_IDX = 6,
            MSSG_QUEUE_START_IDX = 7,
            MSSG_QUEUE_SIZE_IDX = 8,
            NUM_OF_MSSG_IDX = 9,
            GPR0_IDX = 10,
            GPR1_IDX = 11,
            GPR2_IDX = 12,
            GPR3_IDX = 13,
            GPR4_IDX = 14,
            GPR5_IDX = 15,
            GPR6_IDX = 16,
            GPR7_IDX = 17,
            SP_IDX = 18,
            PC_IDX = 19,
            PSR_IDX = 20,
            
            // PCB specifications
            PCB_SIZE = 21,
            DEFAULT_PRIORITY = 128,
            READY_STATE = 1,
            WAITING_STATE = 2,
            // running state ? Edward
            
            // Reasons for process to be waiting
            INPUT_COMPLETION_EVENT = 1,
            OUTPUT_COMPLETION_EVENT = 2,
            
            // System modes
            OS_MODE = 1,
            USER_MODE = 2;
    
    // Declare and initialize simulated main memory and simulated GPRs
    public static final long[]
            MEMORY = new long[(int) MEMORY_SIZE],
            GPR = new long[(int) GPR_SIZE];
    
    
    
    // Declare other components/registers to be simulated and status
    public static long 
            IR,         // Instruction Register
            MAR,        // Memory Address register
            MBR,        // Memory Buffer Register
            PSR,        // Processor Status Register
            Clock,      // Count
            PC,         // Program Counter
            SP,         // Stack pointer
            
            status,     // status
            
            // Pointer to running PCB
            runningPCBPtr = END_OF_LIST,
            // pointer to first PCB in RQ
            RQ = END_OF_LIST,
            // pointer to first PCB in WQ
            WQ = END_OF_LIST,
            // pointer to first free memory address in OS memory
            OSFreeList = END_OF_LIST,
            // pointer to first free memory address in user memory
            userFreeList = END_OF_LIST,
            // Process ID# enumerator
            processID = 1;
    
            
    public static final int
            // Due to the Java's inability to have output parameters in a 
            // function call, an array is used in methods CPU() and
            // fetchOperand() to store and pass values. The descriptor for
            // each element stored in the array is set to its corresponding
            // index in the array.
            MODE = 0,
            REGISTER = 1,
            VALUE = 2,
            ADDRESS = 3;
                        
            
    
    private static final StringBuilder
            OUTPUT_FILE = new StringBuilder();
    
    /**************************************************************************
     * Function: main()
     * 
     * Tasks Performed
     *  main() first calls initializeSystem() to set all hardware components to
     *  their initial state. It then prompts user to enter filename of 
     *  machine code file they want to run.  Next, it calls absoluteLoader()
     *  with the filename as a parameter to load the program into main memory
     *  and sets the PC to the return of the function call. dumpMemory() is 
     *  then called to display contents of the GPRs, SP, PC, and first 40
     *  words in memory. Next, CPU() is called and the instructions are fetched,
     *  decoded, and executed until program Halt.  dumpMemory() is called again
     *  to show contents of the previously mentioned registers and memory 
     *  elements after CPU() call.
     *  
     * Input Parameter
     *  String[] args
     * 
     * Output Parameters
     *  None
     * 
     * Function Return Values
     *  None
     *************************************************************************/
    public static void main(String[] args) {
        // TODO code application logic here
        if((status = initializeSystem()) != OK) {
            printStatus(status);
        }
        
        // Pointer of next process allocated CPU
        runningPCBPtr = END_OF_LIST;  // this is already set in global vars line 135 Edward
        
        while(status != SYSTEM_SHUTDOWN) {
            // Check interrupt and process if needed
            status = checkAndProcessInterrupt();
            if(status == SYSTEM_SHUTDOWN) {
                break;
            }
            if(status != OK) {
                printStatus(status);
                break;
            }
            
            // Dump RQ, WQ, and first 250 locations of user memory
            System.out.println("PRE CPUExecuteProgram() CALL");
            dumpQueue(RQ);
            dumpQueue(WQ);
            dumpMemory(USER_MEM_START_ADDR, 250);
            
            // Set pointer to first PCB in RQ
            runningPCBPtr = selectProcessFromRQ();
            
            // Restore context from PCB for process to be executed
            dispatcher(runningPCBPtr);
            
            // Dump RQ and running PCB context
            dumpQueue(RQ);
            printPCB(runningPCBPtr);
            
            
            // Return of CPUExecuteProgram() is reason for giving up CPU 
            // or error code
            status = CPUExecuteProgram();
            
            // Dump first 250 locations of user memory
            System.out.println("POST CPUExecuteProgram() CALL");
            dumpMemory(USER_MEM_START_ADDR, 250);
            
            //***********TEST*********
            System.out.println("Free user me PTR: " + userFreeList);
            //***********TEST*********
            
            
            // Check why process gave up CPU
            if(status == TIME_SLICE_EXPIRED) {
                System.out.println("TIME SLICE EXP");
                saveContext(runningPCBPtr);
                insertIntoRQ(runningPCBPtr);
                runningPCBPtr = END_OF_LIST;
            }
            else if(status == PROCESS_HALTED) {
                System.out.println("HALT");
                terminateProcess(runningPCBPtr);
                runningPCBPtr = END_OF_LIST;
            }
            else if(status == START_OF_INPUT_OPERATION) {
                System.out.println("INPUT");
                MEMORY[(int) (runningPCBPtr + REASON_FOR_WAITING_IDX)] = INPUT_COMPLETION_EVENT;
                insertIntoWQ(runningPCBPtr);
                runningPCBPtr = END_OF_LIST;
            }
            else if(status == START_OF_OUTPUT_OPERATION) {
                System.out.println("OUTPUT");
                MEMORY[(int) (runningPCBPtr + REASON_FOR_WAITING_IDX)] = OUTPUT_COMPLETION_EVENT;
                insertIntoWQ(runningPCBPtr);
                runningPCBPtr = END_OF_LIST;
            }
            else {  // Error code
                System.out.println("ERROR");
                printStatus(status);
                saveContext(runningPCBPtr);
                insertIntoRQ(runningPCBPtr);
                runningPCBPtr = END_OF_LIST;
            }
        }
        
        System.out.println("OS IS SHUTTING DOWN");
        /*
        try {
            File file = new File("You_HW1_Simulator_output.txt");
            file.createNewFile();
            FileWriter write = new FileWriter(file);
            write.write("");
            write.close();
        } catch (IOException ex) {
            Logger.getLogger(HYPO_MTOPS.class.getName()).log(Level.SEVERE, null, ex);
        }*/
    }
    
    
    /**************************************************************************
     * Function: initializeSystem
     * 
     * Tasks Performed
     *  Initialize all simulated hardware components, except SP, and array
     *  elements to 0. SP is set to MEMORY_SIZE which is the upper limit of
     *  SP in this implementation.  Also, status is set to OK.
     * 
     * Input Parameter
     *  None
     * 
     * Output Parameters
     *  None
     * 
     * Function Return Values
     * `None
     *************************************************************************/
    public static long initializeSystem() {
        // intialize all values in memory to 0
        for(int i = 0; i < MEMORY.length; i++) {
            MEMORY[i] = 0;
        }
        
        // initialize all values in GPRs to 0
        for(int i = 0; i < GPR.length; i++) {
            GPR[i] = 0;
        }
        
        // initialize all registers to 0
        IR = 0;
        MAR = 0;
        MBR = 0;
        PSR = 0;
        Clock = 0;
        PC = 0;
        SP = 0;
        
        // set pointer to start address of user memory block
        userFreeList = USER_MEM_START_ADDR; // sets userFreeList to 4000 Edward
        //
        MEMORY[(int) userFreeList] = END_OF_LIST; // mem[4000] = -1 Edward
        MEMORY[(int) userFreeList + 1] = USER_MEM_SIZE;//mem[4001] = 3000 Edward
        
        OSFreeList = OS_MEM_START_ADDR;
        MEMORY[(int) OSFreeList] = END_OF_LIST; // mem[7000] = -1
        MEMORY[(int) OSFreeList + 1] = OS_MEM_SIZE; //mem[7001] = 3000;
        
        // the process that runs when there are no other processes
        // priority is set to zero so it only runs if it is the last process in the RQ, Edward
        status = createProcess("C:\\Users\\edwar\\Desktop\\OSI\\HW2\\hypo_mtops\\null_process.txt", 0);
        
        
        return status;
    }
    
    /**************************************************************************
     * Function: absoluteLoader
     * 
     * Tasks Performed
     *  Opens file specified by input parameter.  It then loads the contents
     *  into main memory at the proper indices specified in the machine code
     *  file. Upon completion with no errors, the value of PC is returned.
     * 
     * Input Parameter
     *  String  executableFilename
     * 
     * Output Parameters
     *  None
     * 
     * Function Return Values
     *  ER_INVALID_PC_VALUE         // invalid PC value
     *  ER_ADDRESS_OUT_OF_RANGE     // invalid memory index in file
     *  ER_UNABLE_TO_OPEN_FILE      // unable to open file
     *  ER_NO_END_OF_PROGRAM        // no end of program indicator in file
     *  {0, 3999}                    // valid PC value
     *************************************************************************/
    public static long absoluteLoader(String executableFilename) {
        long 
            address,    // memory address where content is to be stored
            content;    // content to be loaded into memory
        
        try {
            File file = new File(executableFilename);
            Scanner programFile = new Scanner(file);
            // Process each line in file until end of program or all lines read
            while(programFile.hasNextLine()) {
                // Set address to first value in line
                address = programFile.nextInt();
                // Set content to next value in line
                content = programFile.nextLong();
                
                // A memory address of less than zero denotes end of program line.
                // The PC value is the content in this line of machine code
                if(address < 0) {
                    // return PC value if valid
                    if(!isValidPC(content)) {
                        return ER_INVALID_PC_VALUE;
                    }
                    programFile.close();
                    return content;
                }
                // Valid memory address. Store content into memory at address 
                else if(address < USER_MEM_START_ADDR) {
                    MEMORY[(int) address] = content;
                }
                // Invalid address
                else {
                    programFile.close();
                    return ER_ADDRESS_OUT_OF_RANGE;
                }
            }
        } catch (FileNotFoundException ex) {
            System.out.println("FILE NOT FOUND EXCEPTION_EDWARD");
            return ER_FILE_NOT_FOUND;
            //Logger.getLogger(HYPO_Machine.class.getName()).log(Level.SEVERE, null, ex);
        }
        // file has no line with -1 as address, end of program indicator
        return ER_NO_END_OF_PROGRAM;
    }
    
    /**************************************************************************
     * Function: CPU()
     * 
     * Tasks Performed
     *  This function simulates the operations a CPU performs.  Instructions
     *  are fetched, decoded, and executed in a manner determined by their
     *  op code and op mode.
     * 
     * Input Parameter
     *  None
     * 
     * Output Parameters
     *  None
     * 
     * Function Return Values
     *  ER_INVALID_PC_VALUE         // invalid PC value
     *  ER_INVALID_OP_MODE          // invalid op mode
     *  ER_FATAL_RUNTIME_ERROR      // runtime error
     *  ER_STACK_OVERFLOW           // stack overflow
     *  ER_STACK_UNDERFLOW          // stack underflow
     *  ER_INVALID_OP_CODE          // invalid op code
     *  ER_ADDRESS_OUT_OF_RANGE     // invalid memory address
     *  ER_INVALID_GPR              // invalid GPR
     *  OK                          // OK
     *************************************************************************/
    public static long CPUExecuteProgram() {
        long counter = TIME_SLICE;
        // Array to hold op modes,op GPR numbers, op value, and op address.
        // This array is the parameter to fetchOperands() call because unlike
        // C, Java has no function output parameters so this array was created
        // to pass and store values
        long[]  
            operand1 = new long[4], // operand 1 elements
            operand2 = new long[4]; // operand 2 elements
        
        // true if opcode is 0, denoting end of program instructions. Op code
        // parsed in fetch cycle and checked in execute
        boolean halt = false;
        
        long 
            opcode,     // variable to hold op code
            remainder,  // variable to hold remainder while parsing
            result;     // variable to hold result
        
        while(!halt) {
            if(counter < 0) {
                return TIME_SLICE_EXPIRED;
            } 
            // The below portion of code is the simulated instruction fetch
            // cycle.  The memory address register is set to the address
            // pointed to by the program counter. The PC is then incremented
            // to then next word in memory.  The memory buffer register is
            // then set to word stored in the address pointed to by MAR.
            // Finally, the IR is set to the contents of MBR
            
            // Not valid PC value
            if(!isValidPC(PC)) {
                return ER_INVALID_PC_VALUE;
            }
            // set MAR to PC and increment PC
            MAR = PC++;
            // set MBR to content in memory at address pointed to by MAR
            MBR = MEMORY[(int) MAR];
            
            // move instruction from memory buffer to instruction register
            IR = MBR;
            
            // The below portion of code is the simulated decode cycle.  Using 
            // integer division and the modulo operation, the op code is first
            // parsed from the instruction and then, the op1 mode, op1 GPR,
            // op2 mode, and op2 GPR are parsed and placed into their 
            // respective array at the proper index.
            
            // Parse opcode from instruction. Note, checking of valid opcode
            // is effectively performed in switch statement of execute cycle
            opcode = IR / 10000;
            remainder = IR % 10000;
            
            // Parse operand modes and GPRs.  Checking of valid mode and valid
            // GPR is performed in fetchOperand() which is called in the
            // execute cycle
            operand1[MODE] = remainder/1000;
            remainder = remainder % 1000;
            
            operand1[REGISTER] = remainder/100;
            remainder = remainder % 100;
            
            operand2[MODE] = remainder/10;
            remainder = remainder % 10;
                
            operand2[REGISTER] = remainder;

            // Below is the simulated execution cycle. Operands are fetched, 
            // when required, and the proper operations are performed on them
            // which are determined by the op code and op mode.
            switch((int) opcode) {
                
                // HALT
                case 0:
                    // set halt to true which will break the while loop
                    halt = true;
                    status = PROCESS_HALTED;
                    break;
                
                // Add.  Requires both operands
                case 1:
                    // fetch op 1
                    if((status = fetchOperand(operand1)) != OK) {
                        return status;
                    }
                    // fetch op 2
                    if((status = fetchOperand(operand2)) != OK) {
                        return status;
                    }
                    
                    // add op1 and op2 values
                    result = operand1[VALUE] + operand2[VALUE];
                    
                    // if register mode, set op1 register to result
                    if(operand1[MODE] == 1) {
                        GPR[(int) operand1[REGISTER]] = result;
                    }
                    // destination operand cannot be an immediate value
                    else if(operand1[MODE] == 6) {
                        return ER_INVALID_OP_MODE;
                    }
                    // store result in memory location pointed by op1 address
                    else {
                        MEMORY[(int) operand1[ADDRESS]] = result;
                    }
                    
                    break;
                
                // Subtract. Requires both operands
                case 2:
                    // fetch op1
                    if((status = fetchOperand(operand1)) != OK) {
                        return status;
                    }
                    // fetch op2
                    if((status = fetchOperand(operand2)) != OK) {
                        return status;
                    }
                    
                    // subtract op2 value from op1 value
                    result = operand1[VALUE] - operand2[VALUE];
                    
                    // if register mode, set op1 register to result
                    if(operand1[MODE] == 1) {
                        GPR[(int) operand1[REGISTER]] = result;
                    }
                    // destination operand cannot be an immediate value.
                    else if(operand1[MODE] == 6) {
                        return ER_INVALID_OP_MODE;
                    }
                    // store result in memory location pointed by op 1 address
                    else {
                        MEMORY[(int) operand1[ADDRESS]] = result;
                    }
                    
                    break;
                
                // Multiply. Requires both operands
                case 3:
                    // fetch op1
                    if((status = fetchOperand(operand1)) != OK) {
                        return status;
                    }
                    // fetch op2
                    if((status = fetchOperand(operand2)) != OK) {
                        return status;
                    }
                    
                    // multiply op1 value with op2 value
                    result = operand1[VALUE] * operand2[VALUE];
                    
                    // if register mode, set op1 register to result
                    if(operand1[MODE] == 1) {
                        GPR[(int) operand1[REGISTER]] = result;
                    }
                    // destination operand cannot be an immediate value.
                    else if(operand1[MODE] == 6) {
                        return ER_INVALID_OP_MODE;
                    }
                    // store result in memory location pointed by op1 address
                    else {
                        MEMORY[(int) operand1[ADDRESS]] = result;
                    }
                    
                    break;
                
                // Divide. Requires both operands
                case 4:
                    // fetch op1 and op2
                    if((status = fetchOperand(operand1)) != OK) {
                        return status;
                    }
                    if((status = fetchOperand(operand2)) != OK) {
                        return status;
                    }
                    
                    // Dividing by 0 is undefined
                    if(operand2[VALUE] == 0) {
                        return ER_FATAL_RUNTIME_ERROR;
                    }
                    
                    // divide op1 value by op2 value
                    result = operand1[VALUE] / operand2[VALUE];
                    
                    // if register mode, set op1 register value to result
                    if(operand1[MODE] == 1) {
                        GPR[(int) operand1[REGISTER]] = result;
                    }
                    // destination operand cannot be an immediate value. 
                    else if(operand1[MODE] == 6) {
                        return ER_INVALID_OP_MODE;
                    }
                    // store result in memory address pointed to by op1 register
                    else {
                        MEMORY[(int) operand1[ADDRESS]] = result;
                    }
                    
                    break;
                
                // Move. Requires both operands
                case 5:
                    // fetch op1 and op2
                    if((status = fetchOperand(operand1)) != OK) {
                        return status;
                    }
                    if((status = fetchOperand(operand2)) != OK) {
                        return status;
                    }
                    
                    // set result to op2 value
                    result = operand2[VALUE];
                    
                    // if register mode, set op1 register to result
                    if(operand1[MODE] == 1) {
                        GPR[(int) operand1[REGISTER]] = result;
                    }
                    // destination operand cannot be an immediate value.
                    else if(operand1[MODE] == 6) {
                        return ER_INVALID_OP_MODE;
                    }
                    // store result in memory address pointed to by op1 register
                    else {
                        MEMORY[(int) operand1[ADDRESS]] = result;
                    }
                    
                    break;
                
                // Branch. No operands. Branch address is value pointed to by PC
                case 6:
                    // set PC to branch address
                    if(!isValidPC(PC = MEMORY[(int) PC])) {
                        return ER_INVALID_PC_VALUE;
                    }
                    
                    break;
                
                // BrOnMinus. Only operand 1 used.  If op1 < 0, branch 
                case 7:   
                    //fetch op 1
                    if((status = fetchOperand(operand1)) != OK) {
                        return status;
                    }
                    
                    // branch taken if op1 value is less than 0
                    if(operand1[VALUE] < 0) {
                        // Set PC to branch address
                        if(!isValidPC(PC = MEMORY[(int) PC])) {
                            return ER_INVALID_PC_VALUE;
                        }
                    }
                    // Branch not taken. Increment PC past branch address
                    else {
                        PC++;
                    }
                    
                    break;
                
                // BrOnPlus. Only operand 1 used.  If op1 > 0, branch
                case 8:
                    // fetch op1
                    if((status = fetchOperand(operand1)) != OK) {
                        return status;
                    }
                    
                    // branch taken if op1 value is greater than 0
                    if(operand1[VALUE] > 0) {
                        // Set PC to branch address
                        if(!isValidPC(PC = MEMORY[(int) PC])) {
                            return ER_INVALID_PC_VALUE;
                        }
                    }
                    // branch not taken. Increment PC past branch address
                    else {
                        PC++;
                    }
                    
                    break;
                
                // BrOnZero. Only operand 1 used.  If op1 == 0, branch
                case 9:
                    // fetch op1
                    if((status = fetchOperand(operand1)) != OK) {
                        return status;
                    }
                    
                    // branch taken if op1 value is 0
                    if(operand1[VALUE] == 0) {
                        // Set PC to branch address
                        if(!isValidPC(PC = MEMORY[(int) PC])) {
                            return ER_INVALID_PC_VALUE;
                        }
                    }
                    // branch not taken. Increment PC past branch address
                    else {
                        PC++;
                    }
                    
                    break;
                
                // Push, if stack is not full. Only op 1 used
                case 10:
                    // fetch op1
                    if((status = fetchOperand(operand1)) != OK) {
                        return status;
                    }
                    if(isStackFull()) {
                        System.out.println(MEMORY[37]);
                        return ER_STACK_OVERFLOW;
                    }
                    
                    System.out.println("PUSH: " + operand1[VALUE]);
                    
                    // Decrement SP and push op1 value onto stack at SP
                    MEMORY[(int) --SP] = operand1[VALUE];
                    
                    break;
                
                // Pop, if stack is not empty. Only op1 used
                case 11:
                    if((status = fetchOperand(operand1)) != OK) {
                        return status;
                    }
                    if(isStackEmpty()) {
                        return ER_STACK_UNDERFLOW;
                    }
                    
                    //System.out.println("POP: " + MEMORY[(int) SP++]);
                    //System.out.println("POP: " + MEMORY[(int) SP++]);
                    
                    // pop top value off of stack and increment SP
                    System.out.println(
                            MEMORY[(int) operand1[(int) ADDRESS]] = MEMORY[(int) SP++]);
                    
                    break;
                
                // System call. Not yet implemented
                case 12:
                    // fetch op1
                    if(!isValidPC(PC)) {
                        return ER_INVALID_PC_VALUE;
                    }
                    
                    long systemCallID = MEMORY[(int) PC++];
                    
                    if((status = systemCall(systemCallID)) != OK) {
                        return status;
                    }
                    
                    break;
                    
                // invalid opcode
                default:    
                    return ER_INVALID_OP_CODE;
            }
            counter -= 25;
        }
        
        // no errors while processing instructions
        return status;
    }
    
    /**************************************************************************
     * Function: fetchOperand()
     * 
     * Tasks Performed
     *  The array passed to this function contains the operand mode and operand
     *  GPR. This function fetches the operand value and address in a manner 
     *  determined by the mode. The op value and op address are then placed 
     *  into the passed array for the calling function to process.
     * 
     * Input Parameter
     *  long[]  operand
     * 
     * Output Parameters
     *  None
     * 
     * Function Return Values
     *  ER_INVALID_GPR              // invalid GPR
     *  ER_ADDRESS_OUT_OF_RANGE     // invalid memory address
     *  ER_INVALID_PC_VALUE         // invalid PC value
     *  ER_INVALID_OP_MODE          // invalid op mode
     *  OK                          // OK
     *************************************************************************/
    public static long fetchOperand(long[] operand) {
        // Invalid GPR
        if(!isValidGPR(operand[REGISTER])) {
            return ER_INVALID_GPR;
        }
        
        switch((int) operand[MODE]) {
            // Register mode.  The value of the operand is in the op register 
            case 1:
                // set to invalid address
                operand[ADDRESS] = -1;
                // set op1 value to the value stored in register 
                operand[VALUE] = GPR[(int) operand[REGISTER]];
                
                break;
            
            // Register deferred mode. Op value address in op register
            case 2:
                // set op address to value stored in op1 register
                operand[ADDRESS] = GPR[(int) operand[REGISTER]];
                if(!isValidProgramAddr(operand[ADDRESS]) &&
                        !isValidUserMemAddr(operand[ADDRESS])) {
                    return ER_ADDRESS_OUT_OF_RANGE;
                }
                
                // set op1 value to the value stored in op address
                operand[VALUE] = MEMORY[(int) operand[ADDRESS]];
                
                break;
            
            // Autoincrement
            case 3: 
                // set memory address to value stored in op1 register
                operand[ADDRESS] = GPR[(int) operand[REGISTER]];
                if(!isValidProgramAddr(operand[ADDRESS]) &&
                        !isValidUserMemAddr(operand[ADDRESS])) {
                    return ER_ADDRESS_OUT_OF_RANGE;
                }
                
                // set op1 value to the value stored in op address
                operand[VALUE] = MEMORY[(int) operand[ADDRESS]];
                
                // increment value stored in op1 register
                GPR[(int) operand[REGISTER]]++;
                
                break;
            
            // Autodecrement
            case 4: 
                // decrement value stored in op1 register
                --GPR[(int) operand[REGISTER]];
                
                // set memory address to value stored in op1 register
                operand[ADDRESS] = GPR[(int) operand[REGISTER]];
                if(!isValidProgramAddr(operand[ADDRESS]) &&
                        !isValidUserMemAddr(operand[ADDRESS])) {
                    return ER_ADDRESS_OUT_OF_RANGE;
                }
                
                break;
            
            // Direct Mode. Op value address is in PC
            case 5: 
                // set op address to address in next word of instruction
                operand[ADDRESS] = MEMORY[(int) PC++];
                if(!isValidProgramAddr(operand[ADDRESS]) &&
                        !isValidUserMemAddr(operand[ADDRESS])) {
                    return ER_ADDRESS_OUT_OF_RANGE;
                }
                
                // set op1 value to value stored in op address
                operand[VALUE] = MEMORY[(int) operand[ADDRESS]];
                
                break;
            
            // Immediate Mode. Op value is in PC
            case 6: 
                // invalid PC
                if(!isValidPC(PC)) {
                    return ER_INVALID_PC_VALUE;
                }
                
                // set address to invalid address
                operand[ADDRESS] = -1;
                
                // set op value to value in PC address
                operand[VALUE] = MEMORY[(int) PC++];
                
                break;
            
            default:
                return ER_INVALID_OP_MODE;
        }
            
        return OK;
    }
    
    
    
    // returns OK, no free memory, ER_INVALID_PC_VALUE         // invalid PC value
    // *  ER_ADDRESS_OUT_OF_RANGE     // invalid memory index in file
    // *  ER_UNABLE_TO_OPEN_FILE      // unable to open file
    // *  ER_NO_END_OF_PROGRAM        // no end of program indicator in file
    public static long createProcess(String filename, long priority) {
        // Create pointer to PCB. Start address of PCB (in OS memory) is return
        // of allocateOSMemory() call
        long PCBPtr = allocateOSMemory(PCB_SIZE);  // pcb size is 21 Edward
        
        // No free OS memory available for PCB
        if(PCBPtr < 0) {
            status = PCBPtr;
            return status;
        }
        
        // initialize PCB
        initializePCB(PCBPtr);
        
        // load program.  PC is is return of absoluteLoader() call
        long pc = absoluteLoader(filename);
        
        if(!isValidPC(pc)) {
            status = pc;
            return status;
        }
        
        // set process PC value in PCB
        MEMORY[(int) (PCBPtr + PC_IDX)] = pc; // sets pc in the process block, Edward
        
        // set pointer to start of program free memory
        long ptr = allocateUserMemory(STACK_SIZE);

        // No free user memory available for stack
        if(ptr < 0) {
            status = ptr;
            return status;
        }
        
        MEMORY[(int) (PCBPtr + SP_IDX)] = ptr + STACK_SIZE;
        MEMORY[(int) (PCBPtr + STACK_START_ADDR_IDX)] = ptr;
        MEMORY[(int) (PCBPtr + STACK_SIZE_IDX)] = STACK_SIZE;
        MEMORY[(int) (PCBPtr + PRIORITY_IDX)] = priority;
        MEMORY[(int) (PCBPtr + PROCESS_STATE_IDX)] = READY_STATE;
        
        status = insertIntoRQ(PCBPtr);
        
        printPCB(PCBPtr);
        dumpMemory(PROGRAM_MEM_START_ADDR, 50);
        
        return status;
    }
    
    public static void initializePCB(long PCBPtr) {
        // Set all values in PCB to 0
        for(int i = 0; i < PCB_SIZE; i++) {
            MEMORY[(int) PCBPtr + i] = 0;
        }
        
        // Set PCB process ID and increment process ID
        MEMORY[(int) PCBPtr + (int) PID_IDX] = processID++;
        
        MEMORY[(int) (PCBPtr + PRIORITY_IDX)] = DEFAULT_PRIORITY;
        MEMORY[(int) (PCBPtr + PROCESS_STATE_IDX)] = READY_STATE;
        MEMORY[(int) (PCBPtr + NEXT_PCB_IDX)] = END_OF_LIST; //this end of list has naver chnaged from -1 so it's not pointing to where it should, should point to 7021 after initialization - Edward
    }
    
    public static void printPCB(long PCBPtr) {
        System.out.println("");
    }
    
    public static void dumpQueue(long queuePtr) {
    }
    
    public static long insertIntoRQ(long PCBPtr) {
        // Invalid PCB adddress
        if(!isValidOSMemAddr(PCBPtr)) {
            System.out.println("Invalid PCB address");
            return ER_INVALID_MEMORY_ADDRESS;
        }
        
        if(RQ == END_OF_LIST) {
            MEMORY[(int) PCBPtr] = END_OF_LIST;
            RQ = PCBPtr;
        }
        else {
            long ptr = RQ;
        
            // Iterate to last PCB in RQ
            while(MEMORY[(int) ptr] != END_OF_LIST) {
                ptr = MEMORY[(int) (ptr + NEXT_PCB_IDX)];
            }
            // Insert PCB at end of RQ
            MEMORY[(int) (ptr + NEXT_PCB_IDX)] = PCBPtr;
            MEMORY[(int) (PCBPtr + NEXT_PCB_IDX)] = END_OF_LIST;
        }
        
        // Set process state to waiting
        MEMORY[(int) (PCBPtr + PROCESS_STATE_IDX)] = READY_STATE;
        
        return OK;
    }
    
    public static long insertIntoWQ(long PCBPtr) {
        // Invalid PCB adddress
        if(!isValidOSMemAddr(PCBPtr)) {
            System.out.println("Invalid PCB address");
            return ER_INVALID_MEMORY_ADDRESS;
        }
        
        // Set process state to waiting
        MEMORY[(int) (PCBPtr + PROCESS_STATE_IDX)] = WAITING_STATE;
        // Set PCB to point to first PCB in WQ 
        MEMORY[(int) (PCBPtr + NEXT_PCB_IDX)] = WQ;
        // Insert PCB at beginning of WQ
        WQ = PCBPtr;
        
        return OK;
    }
    
    public static long selectProcessFromRQ() {
        long nextProcess = RQ;
        RQ = MEMORY[(int) (RQ + NEXT_PCB_IDX)];
        
        return nextProcess;
    }
    
    public static void saveContext(long PCBPtr) {
        // Copy GPR values to PCB GPRs
        for(int idx = 0; idx < GPR_SIZE; idx++) {
            MEMORY[(int) (PCBPtr + GPR0_IDX + idx)] = GPR[idx];
        }
        
        // Set PCB process SP and process PC to SP and PC
        MEMORY[(int) (PCBPtr + SP_IDX)] = SP;
        MEMORY[(int) (PCBPtr + PC_IDX)] = PC;
    }
    
    public static void dispatcher(long PCBPtr) {
        // Copy PCB GPR values to processor GPRs
        for(int idx = 0; idx < GPR_SIZE; idx++) {
            GPR[idx] = MEMORY[(int) (PCBPtr + GPR0_IDX + idx)];
        }
        
        // Set SP and PC to PCB process SP and process PC values
        SP = MEMORY[(int) (PCBPtr + SP_IDX)];
        PC = MEMORY[(int) (PCBPtr + PC_IDX)];
        
        // Set processor status to user mode
        PSR = USER_MODE;
    }
    
    public static void terminateProcess(long PCBPtr) {
        // Free stack memory
        long 
            stackAddr = MEMORY[(int) (PCBPtr + STACK_START_ADDR_IDX)],
            stackSize = MEMORY[(int) (PCBPtr + STACK_SIZE_IDX)];
        
        freeUserMemory(stackAddr, stackSize);
        
        // Free PCB memory
        freeOSMemory(PCBPtr, PCB_SIZE);
    }
    
    // returns OK, no free memory, invalid memory size
    public static long allocateOSMemory(long requestedSize) {
        // no free OS memory
        // here youa re comparing 4000 to -1
        // these will never be equal. You want to compare OSFreeList to mem[9999] not -1 - I think you need a OS_End_Of_List Edward
        if(OSFreeList == END_OF_LIST) {
            return ER_NO_FREE_MEMORY;
        }
        // invalid memory size
        if(requestedSize < 0) {
            return ER_INVALID_MEMORY_SIZE;
        }
        // minimum memory allocation is 2
        if(requestedSize == 1) {
            requestedSize = 2;
        }
        
        // Create pointer to address of first free memory block
        long 
            currentPtr = OSFreeList, 
            previousPtr = END_OF_LIST;
        
        // In the loop below, we check each block of free OS memory until one
        // that can allocate requested memory size is found.
        while(currentPtr != END_OF_LIST) { // this will never end because you are comparing 4000 to -1 for the very first createProcess in Initialize system allocateOS. Do you mean MEMORY[currentPtr]? -Edward
            // found block with requested size
            if(MEMORY[(int) (currentPtr + 1)] == requestedSize) { // >= right? -Edward
                // first block in list
                if(currentPtr == OSFreeList) { 
                    // Set OSFreeList to point to next free OS memory block
                    OSFreeList = MEMORY[(int) currentPtr]; // currentPtr + requested size? -Edward
                    // reset next pointer in allocated block
                    MEMORY[(int) currentPtr] = END_OF_LIST; // this just sets mem[7000] to -1
                    
                    return currentPtr;
                }
                // not first block in list
                else {
                    // set next pointer in previous OS free memory block to next
                    // free block pointer in current block
                    MEMORY[(int) previousPtr] = MEMORY[(int) currentPtr];
                    // reset next pointer in allocated block
                    MEMORY[(int) currentPtr] = END_OF_LIST;
                    
                    return currentPtr;
                }
            }
            // found block larger than requested size
            else if(MEMORY[(int) (currentPtr + 1)] > requestedSize) {
                // first block in list
                if(currentPtr == OSFreeList) {
                    // Move next block pointer
                    MEMORY[(int) (currentPtr + requestedSize)] = // mem[7021] = 7000?
                            MEMORY[(int) currentPtr];
                    // Set size of next block
                    MEMORY[(int) (currentPtr + requestedSize + 1)] =
                            MEMORY[(int) currentPtr + 1] - requestedSize;
                    // Set OSFreelist to point to start of reduced memory block
                    OSFreeList = currentPtr + requestedSize;
                    //Reset next pointer in allocated block
                    MEMORY[(int) currentPtr] = END_OF_LIST;
                    
                    return currentPtr;
                }
                // Not first block
                else {
                    // Move next block pointer
                    MEMORY[(int) (currentPtr + requestedSize)] =
                            MEMORY[(int) currentPtr];
                    // Set size of next block
                    MEMORY[(int) (currentPtr + requestedSize + 1)] =
                            MEMORY[(int) currentPtr + 1] - requestedSize;
                    // Set previous to point to start of reduced memory block
                    // block
                    MEMORY[(int) previousPtr] = currentPtr + requestedSize;
                    //Reset next pointer in allocated block
                    MEMORY[(int) currentPtr] = END_OF_LIST;
                    
                    return currentPtr;
                }
            }
            // Free memory block too small
            else {
                // Move to next block
                previousPtr = currentPtr;
                currentPtr = MEMORY[(int) currentPtr];
            }
        }
        
        // Could not find free memory block large enough
        System.out.println("No free memory available");
        return ER_NO_FREE_MEMORY;
    }
    
    public static long freeOSMemory(long ptr, long size) {
        // If invalid pointer to OS memory
        if(!isValidOSMemAddr(ptr)) {
            System.out.println("Invalid OS memory address");
            return ER_INVALID_MEMORY_ADDRESS;
        }
        
        // Invalid memory size
        if(size < 0) {
            System.out.println("Invalid memory size");
            return ER_INVALID_MEMORY_SIZE;
        }
        // Requesting to free memory block larger than OS memory block
        if(ptr + size >=
                OS_MEM_START_ADDR + OS_MEM_SIZE) {
            System.out.println("Invalid OS memory address");
            return ER_INVALID_MEMORY_ADDRESS;
        }
        // Minimum memory allocation is 2
        if(size == 1) {
            size = 2;
        }
        
        // Set block to point to free block pointed to by OSFreeList
        MEMORY[(int) ptr] = OSFreeList;
        // Set size of free block
        MEMORY[(int) ptr + 1] = size;
        // Set OSFreeList to point to newly freed block
        OSFreeList = ptr;
        
        return OK;
    }
    
    public static long allocateUserMemory(long requestedSize) {
        // no free user memory
        if(userFreeList == END_OF_LIST) {  // same issue -Edward, if 4000 = -1? Not likely -Edward
            return ER_NO_FREE_MEMORY;
        }
        // invalid memory size
        if(requestedSize < 0) {
            return ER_INVALID_MEMORY_SIZE;
        }
        // minimum memory allocation is 2
        if(requestedSize == 1) {
            requestedSize = 2;
        }
        
        // Create pointer to address of first free memory block
        long 
            currentPtr = userFreeList,
            previousPtr = END_OF_LIST;
        
        // In the loop below, we check each block of free OS memory until one
        // that can allocate requested memory size is found.
        while(currentPtr != END_OF_LIST) {
            // found block with requested size
            if(MEMORY[(int) (currentPtr + 1)] == requestedSize) {
                // first block in list
                if(currentPtr == userFreeList) {
                    // Set OSFreeList to point to next free OS memory block
                    userFreeList = MEMORY[(int) currentPtr];
                    // reset next pointer in allocated block
                    MEMORY[(int) currentPtr] = END_OF_LIST;
                    
                    return currentPtr;
                }
                // not first block in list
                else {
                    // set next pointer in previous OS free memory block to next
                    // free block pointer in current block
                    MEMORY[(int) previousPtr] = MEMORY[(int) currentPtr];
                    // reset next pointer in allocated block
                    MEMORY[(int) currentPtr] = END_OF_LIST;
                    
                    return currentPtr;
                }
            }
            // found block larger than requested size
            else if(MEMORY[(int) (currentPtr + 1)] > requestedSize) {
                // first block in list
                if(currentPtr == userFreeList) {
                    // Move next block pointer
                    MEMORY[(int) (currentPtr + requestedSize)] =
                            MEMORY[(int) currentPtr];
                    // Set size of next block
                    MEMORY[(int) (currentPtr + requestedSize + 1)] =
                            MEMORY[(int) currentPtr + 1] - requestedSize;
                    // Set OSFreelist to point to start of reduced memory block
                    userFreeList = currentPtr + requestedSize;
                    //Reset next pointer in allocated block
                    MEMORY[(int) currentPtr] = END_OF_LIST;
                    
                    return currentPtr;
                }
                // Not first block
                else {
                    // Move next block pointer
                    MEMORY[(int) (currentPtr + requestedSize)] =
                            MEMORY[(int) currentPtr];
                    // Set size of next block
                    MEMORY[(int) (currentPtr + requestedSize + 1)] =
                            MEMORY[(int) currentPtr + 1] - requestedSize;
                    // Set previous to point to start of reduced memory block
                    // block
                    MEMORY[(int) previousPtr] = currentPtr + requestedSize;
                    //Reset next pointer in allocated block
                    MEMORY[(int) currentPtr] = END_OF_LIST;
                    
                    return currentPtr;
                }
            }
            // Free memory block too small
            else {
                // Move to next block
                previousPtr = currentPtr;
                currentPtr = MEMORY[(int) currentPtr];
            }
        }
        
        // Could not find free memory block large enough
        System.out.println("No free memory available");
        return ER_NO_FREE_MEMORY;
    }
    
    public static long freeUserMemory(long ptr, long size) {
        if(!isValidUserMemAddr(ptr)) {
            System.out.println("Invalid User memory address");
            return ER_INVALID_MEMORY_ADDRESS;
        }
        
        
        // Invalid memory size
        if(size < 0) {
            System.out.println("Invalid memory size");
            return ER_INVALID_MEMORY_SIZE;
        }
        // Requesting to free memory block larger than user memory block
        if(ptr + size >=
                USER_MEM_START_ADDR + USER_MEM_SIZE) {
            System.out.println("Invalid user memory address");
            return ER_INVALID_MEMORY_ADDRESS;
        }
        // Minimum memory allocation is 2
        if(size == 1) {
            size = 2;
        }
        
        // Set block to point to free block pointed to by UserFreeList
        MEMORY[(int) ptr] = userFreeList;
        // Set size of free block
        MEMORY[(int) ptr + 1] = size;
        // Set userFreeList to point to newly freed block
        userFreeList = ptr;
        
        return OK;
    }
    
    public static long checkAndProcessInterrupt() {
        System.out.println("Please enter the integer that corresponds to"
                + "your interrupt:");
        System.out.println("0:  No interrupt");
        System.out.println("1:  Run program");
        System.out.println("2:  Shutdown system");
        System.out.println("3:  Input operation completion");
        System.out.println("4:  Output operation completion");
        
        boolean isInteger = false;
        int interrupt = -1;
        
        while(!isInteger) {
            try {
                Scanner in = new Scanner(System.in);
                interrupt = in.nextInt();
                isInteger = true;
            } catch (Exception e) {
                System.out.println("Not an integer. Try again.");
            }
        }
        
        switch(interrupt) {
            case 0:   // No interrupt
                status = OK;
                break;
            case 1:   // Run program
                status = ISRRunProgramInterrupt();
                break;
            case 2:   // Shutdown system
                status = ISRShutdownSystem();
                break;
            case 3:   // Input operation completion
                status = ISRInputCompletionInterrupt();
                break;
            case 4:   // Output operation completion
                status = ISROutputCompletionInterrupt();
                break;
            default:
                status = OK;
                System.out.println("Invalid interrupt ID");
                break;
        }
        
       return status; 
    }
    
    public static long ISRRunProgramInterrupt() {
        // Prompt user for filename
        System.out.println("Please enter name of the program file:");
        // Get filename from user
        Scanner in = new Scanner(System.in);
        String filename = in.next();
        
        // Create process with program file
        status = createProcess(filename, DEFAULT_PRIORITY);
        
        return status;
    }
    
    public static long ISRInputCompletionInterrupt() {
        
        System.out.println("Please enter PID of process completing output "
                + "completion interrupt:");
        
        boolean isInteger = false;
        int pid = -1;
        Scanner in = new Scanner(System.in);
        while(!isInteger) {
            try {
                
                pid = in.nextInt();
                isInteger = true;
            } catch (IllegalArgumentException e) {
                System.out.println("Not an integer. Try again.");
            }
        }
        
        // searchAndRemovePCBFromWQ() returns pointer to PCB if found, returns
        // END_OF_LIST if not found
        long PCBPtr = searchAndRemovePCBFromWQ(pid);
        
        // Unable to find PID in WQ
        if(PCBPtr == END_OF_LIST) {
            // Search RQ
            PCBPtr = RQ;
            // Check each PCB in RQ until pid match found or end of RQ is reached
            while(PCBPtr != END_OF_LIST) {
                // Matching pid found
                if(MEMORY[(int) (PCBPtr + PID_IDX)] == pid) {                
                    break;
                }
                
                PCBPtr = MEMORY[(int) (PCBPtr + NEXT_PCB_IDX)];
            }
        }
        
        // Unable to find PID in WQ and RQ
        if(PCBPtr == END_OF_LIST) {
            System.out.println("Invalid PID");
        }
        // PID match found
        else {
            // Get character from user
            char character = (char) in.next().charAt(0);
            // Set PCB GPR1 value to character
            MEMORY[(int) (PCBPtr + GPR1_IDX)] = (long) character;
            // Set process state to ready
            MEMORY[(int) (PCBPtr + PROCESS_STATE_IDX)] = READY_STATE;
            insertIntoRQ(PCBPtr);
        }
        
        return OK;
    }
    
    public static long ISROutputCompletionInterrupt() {
        System.out.println("Please enter PID of process completing input "
                + "completion interrupt:");
        
        boolean isInteger = false;
        int pid = -1;
        Scanner in = new Scanner(System.in);
        while(!isInteger) {
            try {
                pid = in.nextInt();
                isInteger = true;
            } catch (IllegalArgumentException e) {
                System.out.println("Not an integer. Try again.");
            }
        }
        
        // searchAndRemovePCBFromWQ() returns pointer to PCB if found, returns
        // END_OF_LIST if not found
        long PCBPtr = searchAndRemovePCBFromWQ(pid);
        
        // Unable to find PID in WQ
        if(PCBPtr == END_OF_LIST) {
            // search RQ
            PCBPtr = RQ;
            // Check each PCB in RQ until pid match found or end of RQ is reached
            while(PCBPtr != END_OF_LIST) {
                // Matching pid found
                if(MEMORY[(int) (PCBPtr + PID_IDX)] == pid) {                
                    break;
                }
                // Go to next PCB in RQ
                PCBPtr = MEMORY[(int) (PCBPtr + NEXT_PCB_IDX)];
            }
        }
        // Unable to find PID in WQ and RQ
        if(PCBPtr == END_OF_LIST) {
            System.out.println("Invalid PID");
        }
        // PID match found
        else {
            // Print character stored in PCB GPR1
            System.out.println((char) MEMORY[(int) (PCBPtr + GPR1_IDX)]);
            // Set process state to ready
            MEMORY[(int) (PCBPtr + PROCESS_STATE_IDX)] = READY_STATE;
            insertIntoRQ(PCBPtr);
        }
        
        return OK;
    }
    
    public static long ISRShutdownSystem() {
        // Terminate all processes in RQ
        long ptr = RQ;
        
        while(ptr != END_OF_LIST) {
            RQ = MEMORY[(int) (ptr + NEXT_PCB_IDX)];
            terminateProcess(ptr);
            ptr = RQ;
        }
        
        // Terminate all processes in WQ
        ptr = WQ;
        
        while(ptr != END_OF_LIST) {
            WQ = MEMORY[(int) (ptr + NEXT_PCB_IDX)];
            terminateProcess(ptr);
            ptr = WQ;
        }
        
        return SYSTEM_SHUTDOWN;
    }
    
    public static long searchAndRemovePCBFromWQ(long pid) {
        
        // Create pointer to first PCB in WQ
        long 
            currentPCBPtr = WQ,
            previousPCBPtr = END_OF_LIST;
        
        // Check each PCB in WQ until pid match found or end of WQ is reached
        while(currentPCBPtr != END_OF_LIST) {
            // Matching pid found
            if(MEMORY[(int) (currentPCBPtr + PID_IDX)] == pid) {
                // First PCB in WQ
                if(currentPCBPtr == WQ) {
                    // Set WQ to next PCB pointer
                    WQ = MEMORY[(int) (currentPCBPtr + NEXT_PCB_IDX)];
                }
                // Not first PCB in WQ
                else {
                    // Set next PCB pointer in previous PCB to next PCB pointer
                    // in current PCB
                    MEMORY[(int) (previousPCBPtr + NEXT_PCB_IDX)] = 
                            MEMORY[(int) (currentPCBPtr + NEXT_PCB_IDX)];
                }
                
                // Set next PCB pointer in cerrent PCB to END_OF_LIST
                MEMORY[(int) (currentPCBPtr + NEXT_PCB_IDX)] = END_OF_LIST;
                
                return currentPCBPtr;
            }
        }
        
        // No PCB with matching PID found
        System.out.println("PID not found.");
        return END_OF_LIST;
    }
    
    public static long systemCall(long systemCallID) {
        // Set processor status to OS mode
        PSR = OS_MODE;
        
        long status = OK;
        
        switch((int) systemCallID) {
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                System.out.println("MEM_ALLOC SYSCALL SUCCESS");
                status = memAllocSystemCall();
                break;
            case 5:
                status = memFreeSystemCall();
                break;
            case 8:
                status = io_getcSystemCall();
                break;
            case 9:
                status = io_putcSystemCall();
                break;
            default:
                break;
            
        }
        
        PSR = USER_MODE;
        
        return status;
    }
    
    public static long memAllocSystemCall() {
        // Allocate memory from user free list
	// Return status from the function is either the address of 
        // allocated memory or an error code

        // Set size to value in GPR2
        long size = GPR[2];
        
        // Check if size is out of range
        
        // If size is 1, change to 2
        if(size == 1) {
            size = 2;
        }
        
        // Set GPR1 to start address of memory allocated, or negative number
        // if error occured
        GPR[1] = allocateUserMemory(size);
        
        // Set GPR0 to proper status
        if(GPR[1] < 0) {
            // Set GPR0 to error code
            GPR[0] = GPR[1];
        }
        else {
            GPR[0] = OK;
        }
        
        System.out.printf("mem_alloc system call:\n"
                + "GPR0: %d\tGPR1: %d\tGPR2: %d\n",
                GPR[0], GPR[1], GPR[2]);
        
        // Return status
        return GPR[0];
    }
    
    public static long memFreeSystemCall() {
        // Return dynamically allocated memory to the user free list
	// GPR1 has memory address and GPR2 has memory size to be released
	// Return status in GPR0
        
        // Set size to value in GPR2
	long size = GPR[2];

	// Add code to check for size out of range

	// If size is 1, change to 2
        if(size == 1) {
            size = 2;
        }
        
        // Set GPR0 to return status
        GPR[0] = freeUserMemory(GPR[1], size);

        System.out.printf("mem_free system call:\n"
                + "GPR0: %d\tGPR1: %d\tGPR2: %d\n",
                GPR[0], GPR[1], GPR[2]);

	return GPR[0];
    }
    
    public static long io_getcSystemCall() {
        return START_OF_INPUT_OPERATION;
    }
    
    public static long io_putcSystemCall() {
        return START_OF_OUTPUT_OPERATION;
    }
    
    public static boolean isValidPC(long pc) {
        return isValidProgramAddr(pc);
    }
    
    public static boolean isValidGPR(long gpr) {
        return (0 <= gpr &&
                gpr < GPR_SIZE);
    }
    
    public static boolean isStackFull() {
        return (SP <= 
                MEMORY[(int) (runningPCBPtr + STACK_START_ADDR_IDX)]);
    }
    
    public static boolean isStackEmpty() {
        return (SP ==
                MEMORY[(int) (runningPCBPtr + STACK_START_ADDR_IDX)] +
                MEMORY[(int) (runningPCBPtr + STACK_SIZE_IDX)]);
    }
    
    public static boolean isValidProgramAddr(long ptr) {
        return (ptr >= PROGRAM_MEM_START_ADDR &&
                ptr < (PROGRAM_MEM_START_ADDR + PROGRAM_MEM_SIZE));
    }
    
    public static boolean isValidUserMemAddr(long ptr) {
        return (ptr >= USER_MEM_START_ADDR &&
                ptr < (USER_MEM_START_ADDR + USER_MEM_SIZE));
    }
    
    public static boolean isValidOSMemAddr(long ptr) {
        return (ptr >= OS_MEM_START_ADDR &&
                ptr < (OS_MEM_START_ADDR + OS_MEM_SIZE));
    }
    
    /**************************************************************************
     * Function: dumpMemory()
     * 
     * Tasks Performed
     *  This function constructs a formatted String which contains the contents
     *  of the GPRs, SP, PC, and first 40 elements of memory.
     * 
     * Input Parameter
     *  None
     * 
     * Output Parameters
     *  None
     * 
     * Function Return Values
     *  String  // formatted string of state of system
     *************************************************************************/
    public static String dumpMemory(long startAddress, long size) {
        // for construction of string
        StringBuilder memoryDump = new StringBuilder();
        
        // Construct GPR header
        memoryDump.append("GPRs:\t\t");
        for(int i = 0; i < GPR_SIZE; i++) {
            memoryDump.append("G");
            memoryDump.append(i);
            memoryDump.append('\t');
        }
        
        // Construct SP and PC header
        memoryDump.append("SP\tPC\n\t\t");
        
        // append GPR contents
        for(int i = 0; i < GPR_SIZE; i++) {
            memoryDump.append(GPR[i]);
            memoryDump.append('\t');
        }
        
        // append SP and PC content
        memoryDump.append(SP);
        memoryDump.append("\t");
        memoryDump.append(PC);
        memoryDump.append("\n");
        
        // Construct Address header
        memoryDump.append("Address:\t");        
        for(int i = 0; i < 10; i++) {
            memoryDump.append("+");
            memoryDump.append(i);
            memoryDump.append('\t');
        }
        
        // append parameter size memory values beginning at startAddress
        for(int i = 0; i < size; i++) {
            if(i%10 == 0) {
                memoryDump.append("\n");
                memoryDump.append(startAddress + i);
                memoryDump.append(":\t\t");
                memoryDump.append(MEMORY[(int) startAddress + i]);
                memoryDump.append("\t");
            }
            else {
                memoryDump.append(MEMORY[(int) startAddress + i]);
                memoryDump.append("\t");
            }
        }
        
        memoryDump.append("\n");
        System.out.println(memoryDump.toString());
        
        return memoryDump.toString();
    }
    
    /**************************************************************************
     * Function: printStatus()
     * 
     * Tasks Performed
     *  This function prints the status message that is determined by the 
     *  status code. It then calls dumpMemory() to display system state
     * 
     * Input Parameter
     *  long    statusCode
     * 
     * Output Parameters
     *  None
     * 
     * Function Return Values
     *  None
     *************************************************************************/
    public static void printStatus(long statusCode) {
        
        switch ((int) statusCode) {
            case -1:
                System.out.println("ER_UNABLE_TO_OPEN_FILE");
                break;
            case -2: 
                System.out.println("ER_ADDRESS_OUT_OF_RANGE");
                break;
            case -3:
                System.out.println("ER_NO_END_OF_PROGRAM");
                break;
            case -4:
                System.out.println("ER_INVALID_OP_CODE");
                break;
            case -5:
                System.out.println("ER_INVALID_OP_MODE");
                break;
            case -6:  
                System.out.println("ER_INVALID_GPR");
                break;
            case -7:
                System.out.println("ER_INVALID_PC_VALUE");
                break;
            case -8:  
                System.out.println("ER_FATAL_RUNTIME_ERROR");
                break;
            case -9:  
                System.out.println("ER_STACK_OVERFLOW");
                break;
            case -10:
                System.out.println("ER_STACK_UNDERFLOW");
                break;
            case -11:  
                System.out.println("ER_NO_FREE_MEMORY");
                break;
            case -12:  
                System.out.println("ER_INVALID_MEMORY_SIZE");
                break;
            case -13:
                System.out.println("ER_INVALID_MEMORY_ADDRESS");
                break;
            case -14:  
                System.out.println("ER_INVALID_INTERRUPT_ID");
                break;
            default:    
                System.out.println("OK");
                break;
        }
        
        
        //dumpMemory();
    }
}// END OF PROGRAM