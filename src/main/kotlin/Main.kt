import kotlin.math.absoluteValue

const val getPlayerCount = "How many players? (Must be between 1 and 7)"
const val badPlayerCount = "Number of players must be between 1 and 7."

val getAiCount: (Int) -> String = { "How many players are AI? (Must be between 1 and $it)" }
val badAiCount: (Int) -> String = { "Number of AI players must be between 1 and $it" }

const val getNumOfDecks = "How many decks? (Must be between 1 and 8)"
const val badNumOfDecks = "Number of decks must be between 1 and 8."

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

var numOfDecks: Int = 0


fun main() {
    val playerCount = getValidInput(getPlayerCount, badPlayerCount) { it in 1..7 }
    val aiCount = getValidInput(getAiCount(playerCount), badAiCount(playerCount)) { it in 1..playerCount }
    numOfDecks = getValidInput(getNumOfDecks, badNumOfDecks) { it in 1..8 }

    val game = Game(playerCount, aiCount, numOfDecks)

    game.play()
}


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


class Game(private val playerCount: Int, private val aiCount: Int, private val numOfDecks: Int) {

    private val cards = ArrayList<Card>()

    private val deck = List(52) { i -> generateCard(i) }

    private val players = ArrayList<Player>()
    private val eliminatedPlayers = HashSet<Int>()
    private val dealer = Player(money = 0)

    private val handsToPlay = 100

    fun play() {
        onStart()
        var roundNo = 1

        while (roundNo <= handsToPlay && eliminatedPlayers.size < players.size) {
            println("Round no. $roundNo:")

            determineBets(playerCount, aiCount)

            if (eliminatedPlayers.size < players.size) {
                dealCards()
                playRound()
                roundNo++
            }
        }

        onFinish()
    }

    private fun onStart() {
        initDeck()
        initPlayers(playerCount)
        println()
    }

    fun initDeck() {
        cards.clear()
        repeat(numOfDecks) {
            cards.addAll(deck)
        }

        //Remove any cards that already exist in players' hands.
        players.forEach { it.cards.forEach { playerCards -> playerCards.forEach { card -> cards.remove(card) } } }
    }


    private fun initPlayers(playerCount: Int) {
        dealer.cards.add(ArrayList())
        for (i in 1..playerCount) {
            Player().apply {
                players.add(this)
                cards.add(ArrayList())
                bet.add(0)
            }
        }
    }

    private fun dealCard(): Card {
        return cards.getCard(this)
    }

    private fun determineBets(playerCount: Int, aiCount: Int) {
        players.forEachIndexed { i, player ->
            val playerNumber = i + 1

            with(player) {
                if (!eliminatedPlayers.contains(i)) {
                    println("Player $playerNumber, you have $$money.")

                    if (money < 5) {
                        println("Player $playerNumber ran out of money, and is eliminated.")
                        eliminatedPlayers.add(i)
                    } else {
                        if (lastRoundResult == 3) {
                            println("Player $playerNumber, since you pushed last round, your bet of $${players[i].bet} remains.")
                        } else {
                            val maxAmount = if (money >= 20) 20 else money
                            val amountToBet = if (i < (playerCount - aiCount)) {
                                getValidInput(getAmount(maxAmount), badAmount) { it in 5..20 }
                            } else {
                                (5..maxAmount).random()
                            }
                            println("Player $playerNumber bet $$amountToBet.")
                            money -= amountToBet
                            bet[0] = amountToBet
                        }
                    }
                }
            }
        }
    }

    private fun dealCards() {
        players.forEachIndexed { i, player ->
            if (!eliminatedPlayers.contains(i)) {
                player.cards[0].apply {
                    add(dealCard())
                    add(dealCard())
                    println("Player ${i + 1} got ${this.niceToString()}.")
                }
            }
        }

        dealer.cards[0].apply {
            add(dealCard())
            add(dealCard())
            println("The dealer shows ${last()}.")
        }

    }


    private fun playRound() {
        for (i in players.indices) {
            if (!eliminatedPlayers.contains(i)) {
                playTurn(i)
            }
        }

        onRoundFinished()
    }

    private fun playTurn(i: Int) {
        var handNo = 0
        var numHands = players[i].cards.size
        while (handNo < numHands) {
            playHand(i, handNo)
//          println("Player $playerNumber's cards are ${players[i].cards}")
            handNo++
            numHands = players[i].cards.size
        }
    }

    private fun playHand(i: Int, handNo: Int) {
        val playerNumber = i + 1
        var option = 0

        while (option != STAND) {

            with(players[i].cards[handNo]) {
                option = determineOption(players[i], playerNumber, handNo)
                option.run { handleOption(this, i, playerNumber, handNo) }

                val total = getTotal()
                println("Player $playerNumber, ${if (players[i].cards.size > 1) "in hand ${handNo + 1} " else ""}you " +
                        "have ${this.niceToString()}, for a total of $total points.")
            }
        }
    }


    private fun determineOption(player: Player, playerNumber: Int, handNo: Int): Int {
        with(player.cards[handNo]) {
            val total = getTotal()
            return if (total > 21) {
                println("Player $playerNumber, you have gone bust.")
                STAND
            } else if (size == 2 && total == 21) {
                println("Player $playerNumber, you have Blackjack.")
                STAND
            } else {
                val canSplit = size == 2 && containsDuplicates() && player.cards.size < 4
                if (hasAceSplit(player, playerNumber, handNo)) {
                    println("Since player $playerNumber had an ace split, they must stand.")
                    STAND
                } else if (playerNumber - 1 < (playerCount - aiCount)) {
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
                        total < 16 -> HIT
                        else -> STAND
                    }
                }
            }
        }
    }

    private fun hasAceSplit(player: Player, playerNumber: Int, handNo: Int): Boolean {
        var aceSplit = false

        with(player.cards[handNo]) {
            if (size == 1) {
                aceSplit = component1().pipValue == 1

                val newCard = dealCard()
                println("Player $playerNumber was dealt $newCard.")
                player.cards[handNo].add(newCard)
            }
        }

        return aceSplit
    }

    private fun handleOption(option: Int, i: Int, playerNumber: Int, handNo: Int) {
        when (option) {
            HIT -> {
                println("Player $playerNumber hit.")
                val card = dealCard()

                println("Player $playerNumber received $card.")
                players[i].cards[handNo].add(card)
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
            else -> {
            }
        }
    }

    private fun distributeWinnings(playerIndex: Int): Int {
        if (eliminatedPlayers.contains(playerIndex)) {
            return LOSE
        }
        val playerNumber = playerIndex + 1
        val playerCards = players[playerIndex].cards
        val dealerTotalValue = dealer.cards[0].getTotal()

        var lastResult = LOSE
        for (i in 0 until players[playerIndex].cards.size) {
            var moneyPaid = 0
            val totalValue = playerCards[i].getTotal()
            if (players[playerIndex].cards.size > 1) {
                println("For hand ${i + 1}")
            }
            if (totalValue == 21 && playerCards[i].size == 2) {
                moneyPaid = (players[playerIndex].bet[i] * 2.5).toInt()
                println("Player $playerNumber had Blackjack and got a payout of $$moneyPaid.")
                lastResult = WIN

            } else if ((totalValue in (dealerTotalValue + 1)..21) or (totalValue <= 21 && dealerTotalValue > 21)) {
                moneyPaid = players[playerIndex].bet[i] * 2
                println("Player $playerNumber had $totalValue points and got a payout of $$moneyPaid.")
                lastResult = WIN

            } else if (totalValue <= 21 && totalValue == dealerTotalValue) {
                println("Player $playerNumber had a push with $totalValue points.")
                lastResult = PUSH

            } else if (totalValue < dealerTotalValue) {
                println("Player $playerNumber lost $${players[playerIndex].bet[i]} with $totalValue points, which was less than the dealer's score.")

            } else {
                println("Player $playerNumber went bust, with $totalValue points, for a loss of $${players[playerIndex].bet[i]}.")

            }

            players[playerIndex].money += moneyPaid
        }

        return lastResult
    }


    private fun onRoundFinished() {
        with(dealer.cards[0]) {
            println("The dealer reveals ${last()}")
            println("The dealer has a total of ${getTotal()} points.")

            while (getTotal() < 16) {
                add(dealCard())
                println("The dealer hit, and drew ${last()}.")
                println("The dealer now has a total of ${getTotal()} points.")
            }

            if (getTotal() > 21) {
                println("The dealer went bust!")
            }

            players.forEachIndexed { i, player ->
                player.lastRoundResult = distributeWinnings(i)
                player.cards.apply {
                    clear()
                    add(ArrayList())
                }
            }

            clear()
            println()
        }
    }

    private fun onFinish() {
        players.forEach { dealer.money += it.originalMoney - it.money }

        println("The dealer ${if (dealer.money >= 0) "made" else "lost"} $${dealer.money.absoluteValue}.")

        for (i in players.indices) {
            val playerMoney = players[i].money - players[i].originalMoney
            println("Player ${i + 1} ${if (playerMoney >= 0) "made" else "lost"} $${playerMoney.absoluteValue}.")
        }
    }

}

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

fun ArrayList<Card>.getCard(game: Game): Card {
    return if (size > 0) {
        val i = (0 until size).random()
        val card = get(i)
        removeAt(i)
        card
    } else {
        game.initDeck()
        getCard(game)
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

//Can't override toString without extending class
fun ArrayList<Card>.niceToString(): String {
    var s = ""
    when (size) {
        1 -> s += "${component1()}"
        2 -> s += "${component1()} and ${component2()}"
        else -> forEachIndexed { index, card ->
            s += if (index == lastIndex) {
                "and $card"
            } else {
                "$card, "
            }
        }
    }
    return s
}
