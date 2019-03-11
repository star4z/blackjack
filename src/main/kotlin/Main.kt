import kotlin.math.absoluteValue

val cards = ArrayList<Card>()

val deck = List(52) { i -> generateCard(i) }

val players = ArrayList<Player>()
val eliminatedPlayers = HashSet<Int>()
val dealer = Player(money = 0)

fun generateCard(seed: Int): Card {
    val a = seed % 13
    val b = seed / 13
    return Card(
        when (a) {
            0 -> "Ace"
            10 -> "Jack"
            11 -> "Queen"
            12 -> "King"
            else -> (a + 1).toString()
        },
        when (b) {
            0 -> "Spades"
            1 -> "Hearts"
            2 -> "Clubs"
            else -> "Diamonds"
        },
        a + 1
    )
}

const val getPlayerCount = "How many players? (Must be between 1 and 7)"
const val badPlayerCount = "Number of players must be between 1 and 7."

val getAiCount: (Int) -> String = { "How many players are AI? (Must be between 1 and $it)" }
val badAiCount: (Int) -> String = { "Number of AI players must be between 1 and $it" }

const val getNumOfDecks = "How many decks? (Must be between 1 and 8)"
const val badNumOfDecks = "Number of decks must be between 1 and 8."

//val getHandsToPlay: (Int) -> String = { "How many hands will you play? (Must be at least 1 and no more than $it)" }
//const val badHandsToPlay = "You must play at least 1 hand."

val getAmount: (Int) -> String = { "Enter an amount to bet (between \$5 and \$$it):" }
const val badAmount = "Entered amount was not in the indicated range."

val getOption: (HashSet<Int>) -> String = {
    "What will you do?${if (it.contains(1)) "\n" +
            "1. Hit" else ""}${if (it.contains(2)) "\n2. Stand" else ""}${if (it.contains(3)) "\n3. Split" else ""}"
}
const val badOption = "Entry was not a valid option."

const val HIT = 1
const val STAND = 2
const val SPLIT = 3
const val maxOptions = 2

const val LOSE = 0
const val WIN = 1
const val PUSH = 2

data class Card(val faceValue: String, val suite: String, val pipValue: Int) {
    override fun toString(): String {
        return "$faceValue of $suite"
    }
}

data class Player(
    var money: Int = 1000,
    val originalMoney: Int = money,
    val cards: ArrayList<ArrayList<Card>> = ArrayList(),
    var bet: ArrayList<Int> = ArrayList(),
    var lastRoundResult: Int = LOSE
)

var numOfDecks: Int = 0

fun main() {

    val playerCount = getValidInput(getPlayerCount, badPlayerCount) { it in 1..7 }
    val aiCount = getValidInput(getAiCount(playerCount), badAiCount(playerCount)) { it in 1..playerCount }
    numOfDecks = getValidInput(getNumOfDecks, badNumOfDecks) { it in 1..8 }
//    val maxHands = (numOfDecks * 52) / (4 * (playerCount + 1)) //Makes sure that you don't run out of cards
//    val handsToPlay = getValidInput(getHandsToPlay(maxHands), badHandsToPlay) { it in 1..maxHands }
    val handsToPlay = 100

    initDeck()

    dealer.cards.add(ArrayList())
    for (i in 1..playerCount) {
        val player = Player()
        players.add(player)
        player.cards.add(ArrayList())
        player.bet.add(0)
    }

    println()
    var roundNo = 1

    while (roundNo <= handsToPlay && eliminatedPlayers.size < players.size) {
        println("Round no. $roundNo:")

        players.forEachIndexed { i, player ->
            val playerNumber = i + 1
            if (!eliminatedPlayers.contains(i)) {
                println("Player $playerNumber, you have $${player.money}.")

                if (player.money < 5) {
                    println("Player $playerNumber ran out of money, and is eliminated.")
                    eliminatedPlayers.add(i)
                } else {
                    if (player.lastRoundResult == 3) {
                        println("Player $playerNumber, since you pushed last round, your bet of $${players[i].bet} remains.")
                    } else {
                        val maxAmount = if (player.money >= 20) 20 else player.money
                        val amountToBet = if (i < (playerCount - aiCount)) {
                            getValidInput(getAmount(maxAmount), badAmount) { it in 5..20 }
                        } else {
                            (5..maxAmount).random()
                        }
                        println("Player $playerNumber bet $$amountToBet.")
                        player.money -= amountToBet
                        player.bet[0] = amountToBet
                    }
                }
            }
        }

        if (eliminatedPlayers.size < players.size) {

            players.forEachIndexed { i, player ->
                if (!eliminatedPlayers.contains(i)) {
                    player.cards[0].add(dealCard())
                    player.cards[0].add(dealCard())
                    println("Player ${i + 1} got ${player.cards[0]}.")
                }
            }

            dealer.cards[0].add(dealCard())
            dealer.cards[0].add(dealCard())
            println("The dealer shows ${dealer.cards[0][dealer.cards[0].lastIndex]}.")

            for (i in players.indices) {
                if (!eliminatedPlayers.contains(i)) {
                    val playerNumber = i + 1
                    var option = 0

                    var handNo = 0
                    var numHands = players[i].cards.size
                    while (handNo < numHands) {
                        while (option != STAND) {

                            val playerCards = players[i].cards[handNo]

                            var aceSplit = false
                            if (playerCards.size == 1) {
                                aceSplit = playerCards[0].pipValue == 1

                                val newCard = dealCard()
                                println("Player $playerNumber was dealt $newCard.")
                                playerCards.add(newCard)
                            }

                            val total = playerCards.getTotal()
                            println("Player $playerNumber, your cards are $playerCards and you have $total points.")



                            if (total > 21) {
                                println("Player $playerNumber, you have gone bust.")
                                option = STAND
                            } else if (playerCards.size == 2 && total == 21) {
                                println("Player $playerNumber, you have Blackjack.")
                                option = STAND
                            } else {
                                val canSplit =
                                    playerCards.size == 2 && playerCards.containsDuplicates() && players[i].cards.size < 4
                                option = if (aceSplit) {
                                    println("Since player $playerNumber had an ace split, they must stand.")
                                    STAND
                                } else if (i < (playerCount - aiCount)) {
                                    val options = HashSet<Int>()
                                    options.add(1)
                                    options.add(2)
                                    if (canSplit) {
                                        options.add(3)
                                        getValidInput(getOption(options), badOption) { it in 1..maxOptions }
                                    } else {
                                        getValidInput(getOption(options), badOption) { it in 1..maxOptions }
                                    }
                                } else {
                                    when {
                                        canSplit -> SPLIT
                                        playerCards.getTotal() < 16 -> HIT
                                        else -> STAND
                                    }
                                }
                                when (option) {
                                    HIT -> {
                                        println("Player $playerNumber hit.")
                                        val card = dealCard()

                                        println("Player $playerNumber received $card.")
                                        playerCards.add(card)
                                    }
                                    STAND -> println("Player $playerNumber stands.")
                                    SPLIT -> {
                                        println("Player $playerNumber split.")
                                        val cardToMove = players[i].cards[handNo].removeAt(1)
                                        players[i].cards.add(ArrayList())
                                        players[i].cards[handNo + 1].add(cardToMove)

                                        println(
                                            "Player $playerNumber placed an equal bet of ${players[i].bet[handNo]}"
                                                    + " on their next hand."
                                        )
                                        players[i].bet.add(players[i].bet[handNo])
                                        players[i].money -= players[i].bet[handNo + 1]
                                    }
                                }
                            }
                        }
                        println("Player $playerNumber has cards ${players[i].cards}")
                        option = 0
                        handNo++
                        numHands = players[i].cards.size
                    }
                }
            }

            println("The dealer reveals ${dealer.cards[0][dealer.cards[0].lastIndex - 1]}")
            println("The dealer has a total of ${dealer.cards[0].getTotal()} points.")

            while (dealer.cards[0].getTotal() < 16) {
                dealer.cards[0].add(dealCard())
                println("The dealer hit, and drew ${dealer.cards[0][dealer.cards[0].lastIndex]}.")
                println("The dealer's now has a total of ${dealer.cards[0].getTotal()} points.")
            }

            val dealerTotal = dealer.cards[0].getTotal()

            if (dealerTotal > 21) {
                println("The dealer went bust!")
            }
            players.forEachIndexed { i, player ->
                player.lastRoundResult = distributeWinnings(i)
                player.cards.clear()
                player.cards.add(ArrayList())
            }

            roundNo++
            dealer.cards[0].clear()
            println()

        }
    }

    println("The dealer ${if (dealer.money >= 0) "made" else "lost"} $${dealer.money.absoluteValue}.")

    for (i in players.indices) {
        val playerMoney = players[i].money - players[i].originalMoney
        println("Player ${i + 1} ${if (playerMoney >= 0) "made" else "lost"} $${playerMoney.absoluteValue}.")
    }

}

fun initDeck() {
    cards.clear()
    //Shuffle deck?
    repeat(numOfDecks) {
        cards.addAll(deck)
    }
}

fun dealCard(): Card {
    return cards.getCard()
}

fun distributeWinnings(playerIndex: Int): Int {
    if (eliminatedPlayers.contains(playerIndex)) {
        return LOSE
    }
    val playerNumber = playerIndex + 1
    val playerCards = players[playerIndex].cards
    val rounds = playerCards.size - 2
    val dealerTotalValue = dealer.cards[0].getTotal()

    var lastResult = LOSE
    for (i in 0 until players[playerIndex].cards.size) {
        var moneyPaid = 0
        val totalValue = playerCards[i].getTotal()
        if (players[playerIndex].cards.size > 1) {
            println("For hand ${i + 1}")
        }
        if (totalValue == 21 && rounds == 0) {
            moneyPaid = (players[playerIndex].bet[i] * 2.5).toInt()
            println("Player $playerNumber had Blackjack and got a payout of $$moneyPaid.")
            lastResult = WIN

        } else if ((totalValue in (dealerTotalValue + 1)..20) or (totalValue <= 21 && dealerTotalValue > 21)) {
            moneyPaid = players[playerIndex].bet[i] * 2
            println("Player $playerNumber had $totalValue points and got a payout of $$moneyPaid.")
            lastResult = WIN

        } else if (totalValue <= 21 && totalValue == dealerTotalValue) {
            println("Player $playerNumber had a push with $totalValue points.")
            lastResult = PUSH

        } else if (totalValue < dealerTotalValue) {
            println("Player $playerNumber lost with $totalValue points, which was less than the dealer's score.")

        } else {
            println("Player $playerNumber went bust, with $totalValue points.")

        }

        players[playerIndex].money += moneyPaid
        dealer.money += -moneyPaid + players[playerIndex].bet[i]
    }

    return lastResult
}

fun getValidInput(outputString: String, incorrectString: String, conditional: (Int) -> Boolean): Int {
    var enteredValidEntry = false
    var input = -1
    while (!enteredValidEntry) {
        println(outputString)
        val next = readLine()
        input = try {
            next?.toInt() ?: -1
        } catch (e: NumberFormatException) {
            -1
        }

        enteredValidEntry = conditional(input)

        if (!enteredValidEntry) {
            println(incorrectString)
        }
    }

    return input
}


fun ArrayList<Card>.getCard(): Card {
    return if (size > 0) {
        val i = (0 until size).random()
        val card = get(i)
        removeAt(i)
        card
    } else {
        initDeck()
        getCard()
    }
}

fun ArrayList<Card>.getTotal(): Int {
    var total = 0
    var aces = 0
    for (card in this) {
        total += if (card.pipValue > 10) {
            10
        } else {
            card.pipValue
        }
        if (card.pipValue == 1) {
            aces++
        }
    }

    //if adding the ace keeps the total under 21, ace = 11
    for (i in 1..aces) {
        if (total + 10 <= 21) {
            total += 10
        }
    }
    return total
}

fun ArrayList<Card>.containsDuplicates(): Boolean {
    var seenDupes = false
    val seenValues = HashSet<Int>()
    forEach {
        val value = if (it.pipValue > 10) 10 else it.pipValue //all 10-value cards are interchangeable
        if (seenValues.contains(value)) {
            seenDupes = true
        } else {
            seenValues.add(value)
        }
    }
    return seenDupes
}
