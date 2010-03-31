/*
 * Created on Dec 14, 2004
 */
package omschaub.azcvsupdater.utilities;

/**
 * @author omschaub
 *
 */



public class StackX {
    private int maxSize;
    private String[] stackArray;
    private int top;

/** constructor
 * 
 * @param size s
 */
public StackX(int s)
{
    maxSize = s; //set size
    stackArray = new String[maxSize]; //create array
    top = -1;  //no items yet
}


/**put item in top of stack
 * 
 * @param item j
 */
public void push(String j) 
{
    stackArray[++top] = j;  //increment top, insert item
}

/** take item from top of stack
 * 
 * @return item, decrementing top
 */
public String pop()
{
    return stackArray[top--]; //access item, decrement top
}

/** peeks at top of stack
 * 
 * @return item but does not remove it
 */
public String peek()
{
    return stackArray[top];
}

/** true if stack is empty
 * 
 * @return boolean
 */
public boolean isEmpty()
{
    return (top == -1);
}

}