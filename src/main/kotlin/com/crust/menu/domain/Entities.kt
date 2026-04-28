package com.crust.menu.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "menu_version")
class MenuVersion(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var status: String,

    @Column(name = "correlation_id")
    var correlationId: String? = null,

    @OneToMany(mappedBy = "menuVersion", cascade = [CascadeType.ALL], orphanRemoval = true)
    val categories: MutableList<Category> = mutableListOf()
)

@Entity
@Table(name = "category")
class Category(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_version_id", nullable = false)
    val menuVersion: MenuVersion,

    @Column(nullable = false)
    var name: String,

    @Column(name = "correlation_id")
    var correlationId: String? = null,

    @OneToMany(mappedBy = "category", cascade = [CascadeType.ALL], orphanRemoval = true)
    val menuItems: MutableList<MenuItem> = mutableListOf()
)

@Entity
@Table(name = "menu_item")
class MenuItem(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    val category: Category,

    @Column(nullable = false)
    var name: String,

    @Column(name = "base_price")
    var basePrice: BigDecimal? = null,

    @Column(name = "correlation_id")
    var correlationId: String? = null,

    @OneToMany(mappedBy = "menuItem", cascade = [CascadeType.ALL], orphanRemoval = true)
    val modifierGroups: MutableList<ModifierGroup> = mutableListOf()
)

@Entity
@Table(name = "modifier_group")
class ModifierGroup(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    var name: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id")
    val menuItem: MenuItem? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_modifier_id")
    val parentModifier: Modifier? = null,

    @Column(name = "correlation_id")
    var correlationId: String? = null,

    @OneToMany(mappedBy = "modifierGroup", cascade = [CascadeType.ALL], orphanRemoval = true)
    val modifiers: MutableList<Modifier> = mutableListOf()
)

@Entity
@Table(name = "modifier")
class Modifier(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modifier_group_id", nullable = false)
    val modifierGroup: ModifierGroup,

    @Column(nullable = false)
    var name: String,

    @Column(name = "price_adjustment")
    var priceAdjustment: BigDecimal? = null,

    @Column(name = "correlation_id")
    var correlationId: String? = null,

    @OneToMany(mappedBy = "parentModifier", cascade = [CascadeType.ALL], orphanRemoval = true)
    val childModifierGroups: MutableList<ModifierGroup> = mutableListOf()
)
